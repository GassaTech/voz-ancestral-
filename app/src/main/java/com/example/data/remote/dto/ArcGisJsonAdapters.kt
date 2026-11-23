package com.example.data.remote.dto

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

/**
 * Adaptador Moshi explícito (sem reflexão e sem qualifiers KSP2) para os atributos da camada 25
 * do IBAMA/SIGAGEO. Tolera campos ausentes, `null`, strings em branco, campos extras não mapeados
 * e variações escalares comuns em serviços ArcGIS REST (ex.: `terrai_cod` ou `data_atual` como
 * número ou string, e `superficie` como número ou string decimal).
 */
class IndigenousLandAttributesJsonAdapter : JsonAdapter<IndigenousLandAttributesDto>() {

    override fun fromJson(reader: JsonReader): IndigenousLandAttributesDto? {
        if (reader.peek() == JsonReader.Token.NULL) {
            return reader.nextNull()
        }

        var objectId: Long? = null
        var terraiCod: String? = null
        var terraiNom: String? = null
        var etniaNome: String? = null
        var municipio: String? = null
        var ufSigla: String? = null
        var superficie: Double? = null
        var faseTi: String? = null
        var modalidade: String? = null
        var reestudoT: String? = null
        var cr: String? = null
        var faixaFron: String? = null
        var undadmCod: String? = null
        var undadmNom: String? = null
        var undadmSig: String? = null
        var dominioUn: String? = null
        var dataAtual: String? = null
        var epsg: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.selectName(OPTIONS)) {
                0 -> objectId = readFlexibleLong(reader)
                1 -> terraiCod = readFlexibleString(reader)
                2 -> terraiNom = readFlexibleString(reader)
                3 -> etniaNome = readFlexibleString(reader)
                4 -> municipio = readFlexibleString(reader)
                5 -> ufSigla = readFlexibleString(reader)
                6 -> superficie = readFlexibleDouble(reader)
                7 -> faseTi = readFlexibleString(reader)
                8 -> modalidade = readFlexibleString(reader)
                9 -> reestudoT = readFlexibleString(reader)
                10 -> cr = readFlexibleString(reader)
                11 -> faixaFron = readFlexibleString(reader)
                12 -> undadmCod = readFlexibleString(reader)
                13 -> undadmNom = readFlexibleString(reader)
                14 -> undadmSig = readFlexibleString(reader)
                15 -> dominioUn = readFlexibleString(reader)
                16 -> dataAtual = readFlexibleString(reader)
                17 -> epsg = readFlexibleString(reader)
                -1 -> {
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()

        return IndigenousLandAttributesDto(
            objectId = objectId,
            terraiCod = terraiCod,
            terraiNom = terraiNom,
            etniaNome = etniaNome,
            municipio = municipio,
            ufSigla = ufSigla,
            superficie = superficie,
            faseTi = faseTi,
            modalidade = modalidade,
            reestudoT = reestudoT,
            cr = cr,
            faixaFron = faixaFron,
            undadmCod = undadmCod,
            undadmNom = undadmNom,
            undadmSig = undadmSig,
            dominioUn = dominioUn,
            dataAtual = dataAtual,
            epsg = epsg
        )
    }

    override fun toJson(writer: JsonWriter, value: IndigenousLandAttributesDto?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("objectid").value(value.objectId)
        writer.name("terrai_cod").value(value.terraiCod)
        writer.name("terrai_nom").value(value.terraiNom)
        writer.name("etnia_nome").value(value.etniaNome)
        writer.name("municipio_").value(value.municipio)
        writer.name("uf_sigla").value(value.ufSigla)
        writer.name("superficie").value(value.superficie)
        writer.name("fase_ti").value(value.faseTi)
        writer.name("modalidade").value(value.modalidade)
        writer.name("reestudo_t").value(value.reestudoT)
        writer.name("cr").value(value.cr)
        writer.name("faixa_fron").value(value.faixaFron)
        writer.name("undadm_cod").value(value.undadmCod)
        writer.name("undadm_nom").value(value.undadmNom)
        writer.name("undadm_sig").value(value.undadmSig)
        writer.name("dominio_un").value(value.dominioUn)
        writer.name("data_atual").value(value.dataAtual)
        writer.name("epsg").value(value.epsg)
        writer.endObject()
    }

    private fun readFlexibleString(reader: JsonReader): String? = when (reader.peek()) {
        JsonReader.Token.NULL -> reader.nextNull()
        JsonReader.Token.STRING,
        JsonReader.Token.NUMBER -> reader.nextString()
        JsonReader.Token.BOOLEAN -> reader.nextBoolean().toString()
        else -> throw JsonDataException(
            "Valor incompatível para campo textual ArcGIS no token ${reader.peek()} em ${reader.path}"
        )
    }

    private fun readFlexibleDouble(reader: JsonReader): Double? = when (reader.peek()) {
        JsonReader.Token.NULL -> reader.nextNull()
        JsonReader.Token.NUMBER -> reader.nextDouble()
        JsonReader.Token.STRING -> {
            val raw = reader.nextString().trim()
            if (raw.isEmpty()) null else raw.replace(',', '.').toDoubleOrNull()
        }
        else -> throw JsonDataException(
            "Valor incompatível para campo numérico ArcGIS no token ${reader.peek()} em ${reader.path}"
        )
    }

    private fun readFlexibleLong(reader: JsonReader): Long? = when (reader.peek()) {
        JsonReader.Token.NULL -> reader.nextNull()
        JsonReader.Token.NUMBER -> {
            val raw = reader.nextString().trim()
            raw.toLongOrNull() ?: raw.toBigDecimalOrNull()?.toLong()
        }
        JsonReader.Token.STRING -> {
            val raw = reader.nextString().trim()
            if (raw.isEmpty()) null else raw.toLongOrNull()
        }
        else -> throw JsonDataException(
            "Valor incompatível para campo inteiro ArcGIS no token ${reader.peek()} em ${reader.path}"
        )
    }

    private companion object {
        val OPTIONS: JsonReader.Options = JsonReader.Options.of(
            "objectid",
            "terrai_cod",
            "terrai_nom",
            "etnia_nome",
            "municipio_",
            "uf_sigla",
            "superficie",
            "fase_ti",
            "modalidade",
            "reestudo_t",
            "cr",
            "faixa_fron",
            "undadm_cod",
            "undadm_nom",
            "undadm_sig",
            "dominio_un",
            "data_atual",
            "epsg"
        )
    }
}
