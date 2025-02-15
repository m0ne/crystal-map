package com.schwarz.crystaldemo.entity

import com.couchbase.lite.Blob
import com.schwarz.crystalapi.Comment
import com.schwarz.crystalapi.DocId
import com.schwarz.crystalapi.DocIdSegment
import com.schwarz.crystalapi.Entity
import com.schwarz.crystalapi.Field
import com.schwarz.crystalapi.Fields
import com.schwarz.crystalapi.MapWrapper
import com.schwarz.crystalapi.Reduce
import com.schwarz.crystalapi.Reduces
import com.schwarz.crystalapi.query.Queries
import com.schwarz.crystalapi.query.Query

@Entity(database = "mydb_db")
@MapWrapper
@Comment(["Hey, I just met you and this is crazy", "But here's my documentation, so read it maybe"])
@Fields(
    Field(
        name = "type",
        type = String::class,
        defaultValue = "product",
        readonly = true,
        comment = ["Document type"]
    ),
    Field(
        name = "name",
        type = String::class,
        comment = ["contains the product name.", "and other infos"]
    ),
    Field(
        name = "comments",
        type = UserComment::class,
        list = true,
        comment = ["I'm also comfortable with pseudo %2D placeholders"]
    ),
    Field(name = "image", type = Blob::class),
    Field(name = "identifiers", type = String::class, list = true),
    Field(name = "category", type = ProductCategory::class)
)
@Queries(
    Query(fields = ["type"]),
    Query(fields = ["type", "category"])
)
@Reduces(
    Reduce(namePrefix = "Light", include = ["name", "type", "category", "image"]),
    Reduce(namePrefix = "Lighter", include = ["name"], includeQueries = false, includeDocId = false)
)
@DocId("myProduct:%type%:%name%:%custom(name)%")
open class Product {

    companion object {
        @DocIdSegment
        fun custom(name: String?): String = "${name}blub"
    }
}
