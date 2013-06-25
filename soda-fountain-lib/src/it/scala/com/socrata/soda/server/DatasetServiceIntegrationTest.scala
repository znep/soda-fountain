package com.socrata.soda.server

import com.rojoma.json.ast._
import com.rojoma.json.util.JsonUtil
import org.scalatest._

trait DatasetServiceIntegrationTestFixture extends BeforeAndAfterAll with IntegrationTestHelpers { this: Suite =>

  val resourceName = "soda-dataset-service-int-test-0"

  override def beforeAll = {
    val cBody = JObject(Map(
      "resource_name" -> JString(resourceName),
      "name" -> JString("soda integration test"),
      "row_identifier" -> JArray(Seq(JString("col_id"))),
      "columns" -> JArray(Seq(
        column("the ID column", "col_id", Some("this is the ID column"), "number"),
        column("a text column", "col_text", Some("this is a text column"), "text"),
        column("a boolean column", "col_bool", None, "boolean")
      ))
    ))
    val cResponse = dispatch("POST", "dataset", None, None, None,  Some(cBody))

    //publish
    val pResponse = dispatch("PUT", "dataset-copy", Some(resourceName), None, None, None)
    val v = getVersionInSecondaryStore(resourceName)

    //upsert values.  The current time in the last row will cause the data version to increment.
    val uBody = JArray(Seq(
      JObject(Map(("col_id"->JNumber(1)), ("col_text"->JString("row 1")))),
      JObject(Map(("col_id"->JNumber(2)), ("col_text"->JString("row 2")))),
      JObject(Map(("col_id"->JNumber(3)), ("col_text"->JString("row 3 " + System.currentTimeMillis()))))
    ))
    val uResponse = dispatch("POST", "resource", Some(resourceName), None, None,  Some(uBody))
    assert(uResponse.getStatusCode == 200)

    waitForSecondaryStoreUpdate(resourceName, v)
  }

  override def afterAll = {
    println("running the afterAll block")
  }
}

class DatasetServiceIntegrationTest extends IntegrationTest with DatasetServiceIntegrationTestFixture {

  test("update request malformed json returns error response"){
    pendingUntilFixed{
      val response = dispatch("POST", "resource", Option(resourceName), None, None,  Some(JString("this is not json")))
      response.getResponseBody.length must be > (0)
      response.getStatusCode must equal (415)
    }
  }

  test("update request with unexpected format json returns error response"){
    val response = dispatch("POST", "resource", Option(resourceName), None, None,  Some(JArray(Array(JString("this is an array"), JString("why would you post an array?")))))
    response.getStatusCode must equal (400)
  }

  test("soda fountain dataset service getSchema"){
    //get schema
    val gResponse = dispatch("GET", "dataset", Some(resourceName), None, None,  None)
    gResponse.getStatusCode must equal (200)
    val m = JsonUtil.parseJson[Map[String,JValue]](gResponse.getResponseBody)
    m match {
      case Some(map) => {
        map.get("hash").getClass must be (classOf[Some[String]])
        map.get("pk").getClass must be (classOf[Some[String]])
        map.get("schema").getClass must be (classOf[Some[JObject]])
      }
      case None => fail("did not receive schema from soda server")
    }
  }

  test("soda fountain dataset service upsert"){
    val uBody = JArray(Seq(
      JObject(Map(("col_id"->JNumber(1)), ("col_text"->JString("upserted row 1")))),
      JObject(Map(("col_id"->JNumber(3)), ("col_text"->JString("upserted row 3"))))
    ))
    val uResponse = dispatch("POST", "resource", Some(resourceName), None, None,  Some(uBody))
    uResponse.getStatusCode must equal (200)
  }

  test("soda fountain dataset service upsert error case: bad column"){
    val uBody = JArray(Seq(
      JObject(Map(("col_id"->JNumber(1)), ("col_text"->JString("upserted row 1")))),
      JObject(Map(("col_id"->JNumber(2)), ("col_does_not_exist"->JString("row 2")))),
      JObject(Map(("col_id"->JNumber(3)), ("col_text"->JString("upserted row 3"))))
    ))
    val uResponse = dispatch("POST", "resource", Some(resourceName), None, None,  Some(uBody))
    uResponse.getStatusCode must equal (400)
  }

  test("soda fountain dataset service  query") {
    val params = Map(("$query" -> "select * where col_id = 2"))
    val qResponse = dispatch("GET", "resource", Some(resourceName), None, Some(params),  None)
    jsonCompare(qResponse.getResponseBody, """[{col_text:"row 2", col_id: 2.0}]""".stripMargin )
    qResponse.getStatusCode must equal (200)
  }

  test("soda fountain create, upsert, and publish in same request") {
    val rn = "int-test-create-pub-upsert"
    val cBody = JObject(Map(
      "resource_name" -> JString(rn),
      "name" -> JString("soda integration test"),
      "row_identifier" -> JArray(Seq(JString("col_id"))),
      "columns" -> JArray(Seq(
        column("the ID column", "col_id", Some("this is the ID column"), "number"),
        column("a text column", "col_text", Some("this is a text column"), "text"),
        column("a boolean column", "col_bool", None, "boolean")
      )),
      "published" -> JBoolean(true),
      "rows" -> JArray(Seq(
          JObject(Map(("col_id"->JNumber(1)), ("col_text"->JString("row 1")))),
          JObject(Map(("col_id"->JNumber(2)), ("col_text"->JString("row 2")))),
          JObject(Map(("col_id"->JNumber(3)), ("col_text"->JString("row 3 " + System.currentTimeMillis()))))
      ))
    ))
    val cResponse = dispatch("POST", "dataset", None, None, None,  Some(cBody))
    cResponse.getStatusCode must equal (200)
    assert(cResponse.getStatusCode == 200, s"${cResponse.getStatusCode} not OK: ${cResponse.getResponseBody}")
    pendingUntilFixed{
      jsonCompare(cResponse.getResponseBody, """{upsert results}""")
      waitForSecondaryStoreUpdate(rn)
    }
  }

}
