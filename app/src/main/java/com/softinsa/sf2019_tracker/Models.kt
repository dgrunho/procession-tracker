package com.softinsa.sf2019_tracker

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


class Route {

    @SerializedName("name")
    @Expose
    var name: String? = null
    @SerializedName("coordinates")
    @Expose
    var coordinates: List<List<Double>>? = null
    @SerializedName("identifier")
    @Expose
    var identifier: String? = null

}

class Procession(

    @SerializedName("identifier")
    @Expose
    var identifier: String? = null,
    @SerializedName("position")
    @Expose
    var position: Position? = null,
    @SerializedName("date")
    @Expose
    var date: String? = null

)


class RouteId(
    var id: String?
)

class LocationSpec(
    var id: String?,
    var position: Position?,
    var processionPoint: String?
)

class LocationWithIDSpec(
    var position: Position?,
    var deviceid: String?
)

class LocationSpecReturn(
    var id: String?,
    var position: Position?,
    var processionPoint: String?,
    var date: String?
)

class Position(
    var lat: Double?,
    var lng: Double?
)

class Token(
    var PIN: String,
    var ProcessionID: Int
)
