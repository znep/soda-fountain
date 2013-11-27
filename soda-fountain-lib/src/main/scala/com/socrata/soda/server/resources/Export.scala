package com.socrata.soda.server.resources

import com.socrata.http.server.routing.OptionallyTypedPathComponent
import com.socrata.soda.server.id.ResourceName
import com.socrata.soda.server.highlevel.ExportDAO
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import com.socrata.soda.server.SodaUtils
import com.socrata.http.common.util.ContentNegotiation
import com.socrata.http.server.implicits._
import com.socrata.http.server.responses._
import com.socrata.soda.server.util.ETagObfuscator
import java.security.MessageDigest
import java.nio.charset.StandardCharsets
import com.socrata.http.server.util.{Precondition, EntityTag}
import com.socrata.soda.server.errors.{BadParameter, ResourceNotModified, EtagPreconditionFailed}
import com.socrata.soda.server.export.Exporter

case class Export(exportDAO: ExportDAO, etagObfuscator: ETagObfuscator) {
  val log = org.slf4j.LoggerFactory.getLogger(classOf[Export])

  implicit val contentNegotiation = new ContentNegotiation(Exporter.exporters.map { exp => exp.mimeType -> exp.extension }, List("en-US"))

  val headerHashAlg = "SHA1"
  val headerHashLength = MessageDigest.getInstance(headerHashAlg).getDigestLength
  def headerHash(req: HttpServletRequest) = {
    val hash = MessageDigest.getInstance(headerHashAlg)
    hash.update(Option(req.getQueryString).toString.getBytes(StandardCharsets.UTF_8))
    hash.update(255.toByte)
    for(field <- ContentNegotiation.headers) {
      hash.update(field.getBytes(StandardCharsets.UTF_8))
      hash.update(254.toByte)
      for(elem <- req.headers(field)) {
        hash.update(elem.getBytes(StandardCharsets.UTF_8))
        hash.update(254.toByte)
      }
      hash.update(255.toByte)
    }
    hash.digest()
  }

  def export(resourceName: ResourceName, ext: Option[String])(req: HttpServletRequest)(resp: HttpServletResponse) {
    exportCopy(resourceName, "published", ext)(req)(resp)
  }

  def exportCopy(resourceName: ResourceName, copy: String, ext: Option[String])(req: HttpServletRequest)(resp: HttpServletResponse) {
    // Etags generated by this system are the obfuscation of the etag from upstream plus
    // the hash of the contents of the header fields naemd by ContentNegotiation.headers.
    // So, when we receive etags in an if-none-match from the client
    //   1. decrypt the tags
    //   2. extract our bit of the data
    //   3. hash our headers and compare, dropping the etag completely if the hash is different
    //   4. Passing the remaining (decrypted and hash-stripped) etags upstream.
    //
    // For if-match it's the same, only we KEEP the ones that match the hash (and if that eliminates
    // all of them, then we "expectation failed" before ever passing upward to the data-coordinator)
    val limit = Option(req.getParameter("limit")).map { limStr =>
      try {
        limStr.toLong
      } catch {
        case e: NumberFormatException =>
          SodaUtils.errorResponse(req, BadParameter("limit", limStr))(resp)
          return
      }
    }

    val offset = Option(req.getParameter("offset")).map { offStr =>
      try {
        offStr.toLong
      } catch {
        case e: NumberFormatException =>
          SodaUtils.errorResponse(req, BadParameter("offset", offStr))(resp)
          return
      }
    }

    val suffix = headerHash(req)
    val precondition = req.precondition.map(etagObfuscator.deobfuscate)
    def prepareTag(etag: EntityTag) = etagObfuscator.obfuscate(etag.append(suffix))
    precondition.filter(_.endsWith(suffix)) match {
      case Right(newPrecondition) =>
        val passOnPrecondition = newPrecondition.map(_.dropRight(suffix.length))
        req.negotiateContent match {
          case Some((mimeType, charset, language)) =>
            val exporter = Exporter.exportForMimeType(mimeType)
            exportDAO.export(resourceName, passOnPrecondition, limit, offset, copy) {
              case ExportDAO.Success(schema, newTag, rows) =>
                resp.setStatus(HttpServletResponse.SC_OK)
                resp.setHeader("Vary", ContentNegotiation.headers.mkString(","))
                newTag.foreach { tag =>
                  ETag(prepareTag(tag))(resp)
                }
                exporter.export(resp, charset, schema, rows)
              case ExportDAO.PreconditionFailed =>
                SodaUtils.errorResponse(req, EtagPreconditionFailed)(resp)
              case ExportDAO.NotModified(etags) =>
                SodaUtils.errorResponse(req, ResourceNotModified(etags.map(prepareTag), Some(ContentNegotiation.headers.mkString(","))))(resp)
            }
          case None =>
            // TODO better error
            NotAcceptable(resp)
        }
      case Left(Precondition.FailedBecauseNoMatch) =>
        SodaUtils.errorResponse(req, EtagPreconditionFailed)(resp)
    }
  }

  case class publishedService(resourceAndExt: OptionallyTypedPathComponent[ResourceName]) extends SodaResource {
    override def get = export(resourceAndExt.value, resourceAndExt.extension.map(Exporter.canonicalizeExtension))
  }

  case class service(resource: ResourceName, copyAndExt: OptionallyTypedPathComponent[String]) extends SodaResource {
    override def get = exportCopy(resource, copyAndExt.value, copyAndExt.extension.map(Exporter.canonicalizeExtension))
  }

  def extensions(s: String) = Exporter.exporterExtensions.contains(Exporter.canonicalizeExtension(s))
}
