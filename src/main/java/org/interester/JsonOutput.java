package org.interester;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@UtilityClass
public class JsonOutput {

    private final ObjectWriter PRETTY_WRITER = JsonMapper.builder()
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
            .build()
            .writerWithDefaultPrettyPrinter();

    @SneakyThrows
    public void write(Object value, OutputStream out) {
        PRETTY_WRITER.writeValue(out, value);
        out.write("\n".getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
