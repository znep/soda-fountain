package com.socrata.soda.server.services

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import com.socrata.http.server.{responses, HttpResponse}
import scala.Some
import com.rojoma.json.ast._
import com.socrata.http.server.responses._
import com.socrata.http.server.implicits._
import com.socrata.datacoordinator.client.DataCoordinatorClient
import com.socrata.soda.server.persistence.NameAndSchemaStore
import com.socrata.querycoordinator.client.QueryCoordinatorClient
import dispatch._
import com.typesafe.config.ConfigFactory
import com.socrata.datacoordinator.client.DataCoordinatorClient.SchemaSpec
import com.ning.http.client.Response
import org.apache.log4j.PropertyConfigurator
import com.socrata.thirdparty.typesafeconfig.Propertizer
import com.socrata.soql.brita.IdentifierFilter

object SodaService {
  val config = ConfigFactory.load().getConfig("com.socrata.soda-fountain")
}

trait SodaService {

  PropertyConfigurator.configure(Propertizer("log4j", SodaService.config.getConfig("log4j")))
  val dc : DataCoordinatorClient
  val store : NameAndSchemaStore
  val qc : QueryCoordinatorClient
  val mockUser = "soda-server-community-edition"
  val log = org.slf4j.LoggerFactory.getLogger(classOf[SodaService])

  def schemaHash(r: HttpServletRequest) = Option(r.getParameter("schema"))

  def sendErrorResponse(message: String, errorCode: String, httpCode: HttpServletResponse => Unit, data: Option[JValue], logTags: String*) = {
    val messageAndCode = Map[String, JValue](
      "message" -> JString(message),
      "errorCode" -> JString(errorCode)
    )
    val errorMap = data match {
      case Some(d) => messageAndCode + ("data" -> d)
      case None => messageAndCode
    }
    log.info(s"${logTags.mkString(" ")} responding with error ${errorCode}")
    httpCode ~> ContentType("application/json; charset=utf-8") ~> Content(JObject(errorMap).toString)
  }

  def validName(name: String) = IdentifierFilter(name).equals(name)
  def sendInvalidNameError(name:String, request: HttpServletRequest) = sendErrorResponse("invalid name", "invalid.name", BadRequest, Some(JString(name)), request.getRequestURI, request.getMethod)

  def passThroughResponse(f: Future[Either[Throwable,Response]], logTags: String*): HttpServletResponse => Unit = {
    f() match {
      case Right(response) => passThroughResponse(response, logTags:_*)
      case Left(th) => sendErrorResponse(th.getMessage, "internal.error", InternalServerError, None, logTags:_*)
    }
  }
  def passThroughResponse(response: Response, logTags: String*): HttpServletResponse => Unit = {
    log.info(s"${logTags.mkString(" ")} returning ${response.getStatusText} - ${response.getStatusCode}")
    responses.Status(response.getStatusCode) ~>  ContentType(response.getContentType) ~> Content(response.getResponseBody)
  }

  def pkValue(rowId: String, schema: SchemaSpec) = {
    val pkType = schema.schema.get(schema.pk).getOrElse(throw new Exception("Primary key column not represented in schema. This should not happen."))
    pkType match {
      case "text" => Left(rowId)
      case "number" => Right(BigDecimal(rowId))
      case "row_identifier" => Right(BigDecimal(rowId))
      case _ => throw new Exception("Primary key column not text or number")}
  }

  def notSupported(id:String)(request:HttpServletRequest): HttpServletResponse => Unit = ???
  //surely this can be improved, but changing it to a String* vararg makes the router angry.
  def notSupported2(id:String, part2:String)(request:HttpServletRequest): HttpServletResponse => Unit = ???

  def withDatasetId(resourceName: String)(f: String => HttpResponse): HttpResponse = {
    val rnf = store.translateResourceName(resourceName)
    rnf() match {
      case Right(datasetId) => f(datasetId)
      case Left(err) => sendErrorResponse(err, "dataset.not.found", BadRequest, None, resourceName)
    }
  }
  def withDatasetSchema(datasetId: String)(f: SchemaSpec => HttpResponse): HttpResponse = {
    val sf = dc.getSchema(datasetId)
    sf() match {
      case Right(schema) => f(schema)
      case Left(err) => sendErrorResponse(err, "schema.not-found", NotFound, None, datasetId)
    }
  }
}
