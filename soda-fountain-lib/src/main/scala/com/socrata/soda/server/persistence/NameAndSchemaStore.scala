package com.socrata.soda.server.persistence

import com.socrata.soda.server.id.{ColumnId, DatasetId, ResourceName}
import com.socrata.soql.environment.ColumnName
import com.socrata.soql.types.SoQLType
import com.rojoma.json.util.AutomaticJsonCodecBuilder
import com.socrata.soda.server.util.AdditionalJsonCodecs._
import com.socrata.soda.server.util.schema.SchemaSpec

// TODO: this needs to expose a notion of transactions
trait NameAndSchemaStore {
  def addResource(newRecord: DatasetRecord)
  def removeResource(resourceName: ResourceName)
  def translateResourceName(resourceName: ResourceName): Option[MinimalDatasetRecord]
  def lookupDataset(resourceName: ResourceName): Option[DatasetRecord]

  def addColumn(datasetId: DatasetId, columnSystemId: ColumnId, columnFieldName: ColumnName) : Unit
  def updateColumnFieldName(datasetId: DatasetId, columnId: ColumnName, newFieldName: ColumnName) : Unit
  def dropColumn(datasetId: DatasetId, columnId: ColumnId) : Unit
}

trait DatasetRecordLike {
  type ColumnRecordT <: ColumnRecordLike

  val resourceName: ResourceName
  val systemId: DatasetId
  val columns: Seq[ColumnRecordT]
  val locale: String
  val schemaHash: String
  val primaryKey: ColumnId

  lazy val columnsByName = columns.groupBy(_.fieldName).mapValues(_.head)
  lazy val minimalSchemaByName = columnsByName.mapValues(_.typ)
  lazy val columnsById = columns.groupBy(_.id).mapValues(_.head)
  lazy val minimalSchemaById = columnsById.mapValues(_.typ)
  lazy val schemaSpec = SchemaSpec(schemaHash, locale, primaryKey, minimalSchemaById)
}

trait ColumnRecordLike {
  val id: ColumnId
  val fieldName: ColumnName
  val typ: SoQLType
}

// A minimal dataset record is a dataset record minus the name and description columns,
// which are unnecessary for most operations.
case class MinimalColumnRecord(id: ColumnId, fieldName: ColumnName, typ: SoQLType) extends ColumnRecordLike
object MinimalColumnRecord {
  implicit val jCodec = AutomaticJsonCodecBuilder[MinimalColumnRecord]
}
case class MinimalDatasetRecord(resourceName: ResourceName, systemId: DatasetId, locale: String, schemaHash: String, primaryKey: ColumnId, columns: Seq[MinimalColumnRecord]) extends DatasetRecordLike {
  type ColumnRecordT = MinimalColumnRecord
}
object MinimalDatasetRecord {
  implicit val jCodec = AutomaticJsonCodecBuilder[MinimalDatasetRecord]
}

case class ColumnRecord(id: ColumnId, fieldName: ColumnName, typ: SoQLType, name: String, description: String) extends ColumnRecordLike
object ColumnRecord {
  implicit val jCodec = AutomaticJsonCodecBuilder[ColumnRecord]
}
case class DatasetRecord(resourceName: ResourceName, systemId: DatasetId, name: String, description: String, locale: String, schemaHash: String, primaryKey: ColumnId, columns: Seq[ColumnRecord]) extends DatasetRecordLike {
  type ColumnRecordT = ColumnRecord
}
object DatasetRecord {
  implicit val jCodec = AutomaticJsonCodecBuilder[DatasetRecord]
}
