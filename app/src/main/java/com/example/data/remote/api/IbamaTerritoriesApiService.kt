package com.example.data.remote.api

import com.example.data.remote.dto.ArcGisFeatureQueryResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface IbamaTerritoriesApiService {

    @GET("query")
    suspend fun queryIndigenousLands(
        @Query("where") where: String = ArcGisTerritoriesQuerySpec.DEFAULT_WHERE,
        @Query("outFields") outFields: String = ArcGisTerritoriesQuerySpec.DEFAULT_OUT_FIELDS,
        @Query("returnGeometry") returnGeometry: Boolean = ArcGisTerritoriesQuerySpec.DEFAULT_RETURN_GEOMETRY,
        @Query("orderByFields") orderByFields: String = ArcGisTerritoriesQuerySpec.DEFAULT_ORDER_BY_FIELDS,
        @Query("resultOffset") resultOffset: Int = 0,
        @Query("resultRecordCount") resultRecordCount: Int = ArcGisTerritoriesQuerySpec.DEFAULT_PAGE_SIZE,
        @Query("f") format: String = ArcGisTerritoriesQuerySpec.DEFAULT_FORMAT
    ): ArcGisFeatureQueryResponseDto
}
