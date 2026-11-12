package ctpcliente.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import ctpcliente.error.CtpException;
import ctpcliente.error.CtpOwsException;
import ctpcliente.error.CtpRespuestaVaciaException;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Respuestas {

    static final ObjectMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private static final Pattern TEXTO_EXCEPCION =
            Pattern.compile("<(ows:ExceptionText|ServiceException)(?:\\s[^>]*)?>(.*?)</\\1>",
                    Pattern.DOTALL);
    private static final Pattern CODIGO_EXCEPCION = Pattern.compile("(?:exceptionCode|code)=\"([^\"]+)\"");
    private static final Pattern ESPACIOS = Pattern.compile("\\s+");

    private Respuestas() {
    }

    static JsonNode json(String cuerpo, URI uri) {
        if (cuerpo == null || cuerpo.isBlank()) {
            throw new CtpRespuestaVaciaException(uri);
        }
        String inicio = cuerpo.stripLeading();
        if (inicio.startsWith("<")) {
            throw errorXml(inicio, uri);
        }
        try {
            return MAPPER.readTree(cuerpo);
        } catch (JsonProcessingException e) {
            throw new CtpException("JSON invalido desde " + uri, e);
        }
    }

    static Optional<CtpOwsException> excepcionOws(String cuerpo) {
        if (cuerpo == null || !(cuerpo.contains("ExceptionReport") || cuerpo.contains("ServiceException"))) {
            return Optional.empty();
        }
        Matcher texto = TEXTO_EXCEPCION.matcher(cuerpo);
        String mensaje = texto.find() ? ESPACIOS.matcher(texto.group(2)).replaceAll(" ").strip() : "Error de GeoServer";
        Matcher codigo = CODIGO_EXCEPCION.matcher(cuerpo);
        return Optional.of(new CtpOwsException(codigo.find() ? codigo.group(1) : null, mensaje));
    }

    static CtpException errorXml(String xml, URI uri) {
        return excepcionOws(xml)
                .map(CtpException.class::cast)
                .orElseGet(() -> new CtpException("Respuesta XML inesperada de " + uri));
    }

    static void exigirSinExcepcionOws(String cuerpo) {
        Optional<CtpOwsException> e = excepcionOws(cuerpo);
        if (e.isPresent()) {
            throw e.get();
        }
    }
}
