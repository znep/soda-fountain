package com.socrata.soda.clients.datacoordinator

import com.socrata.http.client.{Response, RequestBuilder, HttpClient}
import com.socrata.soda.server.id.{SecondaryId, DatasetId}
import com.socrata.http.server.routing.HttpMethods
import com.rojoma.json.ast.{JString, JArray, JValue}
import com.socrata.soda.server.util.schema.SchemaSpec

abstract class HttpDataCoordinatorClient(httpClient: HttpClient) extends DataCoordinatorClient {
  import DataCoordinatorClient._

  val log = org.slf4j.LoggerFactory.getLogger(classOf[DataCoordinatorClient])

  def hostO(instance: String): Option[RequestBuilder]
  def createUrl(host: RequestBuilder) = host.p("dataset")
  def mutateUrl(host: RequestBuilder, datasetId: DatasetId) = host.p("dataset", datasetId.underlying)
  def schemaUrl(host: RequestBuilder, datasetId: DatasetId) = host.p("dataset", datasetId.underlying, "schema")
  def secondaryUrl(host: RequestBuilder, secondaryId: SecondaryId, datasetId: DatasetId) = host.p("secondary-manifest", secondaryId.underlying, datasetId.underlying)

  def withHost[T](instance: String)(f: RequestBuilder => T): T =
    hostO(instance) match {
      case Some(host) => f(host)
      case None => throw new Exception("could not connect to data coordinator")
    }

  def withHost[T](datasetId: DatasetId)(f: RequestBuilder => T): T =
    withHost(datasetId.nativeDataCoordinator)(f)

  def propagateToSecondary(datasetId: DatasetId, secondaryId: SecondaryId): Unit =
    withHost(datasetId) { host =>
      val r = secondaryUrl(host, secondaryId, datasetId).method(HttpMethods.POST).get // ick
      for (response <- httpClient.execute(r)) yield {
        response.resultCode match {
          case 200 => // ok
          case _ => throw new Exception("could not propagate to secondary")
        }
      }
    }

  def getSchema(datasetId: DatasetId): Option[SchemaSpec] =
    withHost(datasetId) { host =>
      val request = schemaUrl(host, datasetId).get
      for (response <- httpClient.execute(request)) yield {
        if(response.resultCode == 200) {
          val result = response.asValue[SchemaSpec]()
          if(!result.isDefined) throw new Exception("Unable to interpret data coordinator's response for " + datasetId + " as a schemaspec?")
          result
        } else if(response.resultCode == 404) {
          None
        } else {
          throw new Exception("Unexpected result from server: " + response.resultCode)
        }
      }
    }

  // TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
  // TODO                                                                  TODO
  // TODO :: ALL THESE NEED TO HANDLE ERRORS FROM THE DATA COORDINATOR! :: TODO
  // TODO                                                                  TODO
  // TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO

  protected def sendScript[T]( rb: RequestBuilder, script: MutationScript) (f: ((Response) => T)): T = {
    val request = rb.json(script.it)
    for (r <- httpClient.execute(request)) yield f(r)
  }

  def create(instance: String,
             user: String,
             instructions: Option[Iterator[DataCoordinatorInstruction]],
             locale: String = "en_US") : (DatasetId, Iterable[JValue]) = {
    withHost(instance) { host =>
      val createScript = new MutationScript(user, CreateDataset(locale), instructions.getOrElse(Array().iterator))
      sendScript(createUrl(host), createScript){ response : Response =>
        log.info("TODO: Handle errors from the data-coordinator")
        val idAndReports = response.asValue[JArray]()
        idAndReports match {
          case Some(JArray(Seq(JString(datasetId), _*))) => (DatasetId(datasetId), idAndReports.get.tail)
          case None => throw new Exception("unexpected response from data coordinator")
        }
      }
    }
  }

  def update[T](datasetId: DatasetId, schemaHash: String, user: String, instructions: Iterator[DataCoordinatorInstruction])(f: Iterator[JValue] => T): T = {
    log.info("TODO: update should decode the row op report into something higher-level than JValues")
    withHost(datasetId) { host =>
      val updateScript = new MutationScript(user, UpdateDataset(Some(schemaHash)), instructions)
      sendScript(mutateUrl(host, datasetId), updateScript) { r =>
        log.info("TODO: Handle errors from the data-coordinator")
        log.info("TODO: particularly \"schema mismatch\"")
        f(r.asArray[JValue]())
      }
    }
  }

  def copy[T](datasetId: DatasetId, copyData: Boolean, user: String, instructions: Iterator[DataCoordinatorInstruction])(f: Iterator[JValue] => T): T = {
    log.info("TODO: copy should decode the row op report into something higher-level than JValues")
    withHost(datasetId) { host =>
      val createScript = new MutationScript(user, CopyDataset(copyData, None), instructions)
      sendScript(mutateUrl(host, datasetId), createScript) { r =>
        log.info("TODO: Handle errors from the data-coordinator")
        f(r.asArray[JValue]())
      }
    }
  }
  def publish[T](datasetId: DatasetId, snapshotLimit:Option[Int], user: String, instructions: Iterator[DataCoordinatorInstruction])(f: Iterator[JValue] => T): T = {
    log.info("TODO: publish should decode the row op report into something higher-level than JValues")
    withHost(datasetId) { host =>
      val pubScript = new MutationScript(user, PublishDataset(snapshotLimit, None), instructions)
      sendScript(mutateUrl(host, datasetId), pubScript) { r =>
        log.info("TODO: Handle errors from the data-coordinator")
        f(r.asArray[JValue]())
      }
    }
  }
  def dropCopy[T](datasetId: DatasetId, user: String, instructions: Iterator[DataCoordinatorInstruction])(f: Iterator[JValue] => T): T = {
    log.info("TODO: dropCopy should decode the row op report into something higher-level than JValues")
    withHost(datasetId) { host =>
      val dropScript = new MutationScript(user, DropDataset(None), instructions)
      sendScript(mutateUrl(host, datasetId), dropScript) { r =>
        log.info("TODO: Handle errors from the data-coordinator")
        f(r.asArray[JValue]())
      }
    }
  }

  // Pretty sure this is completely wrong
  def deleteAllCopies[T](datasetId: DatasetId, schema: Option[String], user: String)(f: Iterator[JValue] => T): T = {
    log.info("TODO: deleteAllCopies should decode the row op report into something higher-level than JValues")
    withHost(datasetId) { host =>
      val deleteScript = new MutationScript(user, DropDataset(schema), Iterator.empty)
      sendScript(mutateUrl(host, datasetId).method(HttpMethods.DELETE), deleteScript) { r =>
        log.info("TODO: Handle errors from the data-coordinator")
        f(r.asArray[JValue]())
      }
    }
  }

  def checkVersionInSecondary(datasetId: DatasetId, secondaryId: SecondaryId): VersionReport = {
    withHost(datasetId) { host =>
      val request = secondaryUrl(host, secondaryId, datasetId).get
      for (r <- httpClient.execute(request)) yield {
        log.info("TODO: Handle errors from the data-coordinator")
        val oVer = r.asValue[VersionReport]()
        oVer match {
          case Some(ver) => ver
          case None => throw new Exception("version not found")
        }
      }
    }
  }
}