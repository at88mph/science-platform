package org.opencadc.skaha.session;

import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import org.jetbrains.annotations.NotNull;
import org.json.JSONWriter;
import org.opencadc.skaha.K8SUtil;

import java.io.IOException;
import java.io.Writer;

public class UserSession<T extends SessionType> {
    final T sessionType;
    final String name;

    void submit() {

    }

    /**
     * Write the session information to the provided Writer as JSON.
     * @param writer    The Writer to which the JSON representation of the session will be written.
     */
    void toJSON(@NotNull final Writer writer) throws IOException {
        JSONWriter jsonWriter = new JSONWriter(writer);
        jsonWriter.object();
        // Add session properties here
        jsonWriter.endObject();
    }

    public V1Job toJobSpec() {
        final V1Job jobSpec = new V1Job();
        final V1ObjectMeta metadata = new V1ObjectMeta();
        metadata.setNamespace(K8SUtil.getWorkloadNamespace());
        metadata.setName(this.name);

        return jobSpec;
    }
}
