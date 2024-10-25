package org.opencadc.skaha.session;

import ca.nrc.cadc.util.StringUtil;
import io.kubernetes.client.openapi.models.V1HTTPIngressPath;
import io.kubernetes.client.openapi.models.V1HTTPIngressRuleValue;
import io.kubernetes.client.openapi.models.V1Ingress;
import io.kubernetes.client.openapi.models.V1IngressBackend;
import io.kubernetes.client.openapi.models.V1IngressRule;
import io.kubernetes.client.openapi.models.V1IngressServiceBackend;
import io.kubernetes.client.openapi.models.V1IngressSpec;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ServiceBackendPort;
import io.kubernetes.client.proto.Meta;
import io.kubernetes.client.proto.V1Networking;
import io.kubernetes.client.util.Yaml;
import java.util.Collections;


public class SessionIngressBuilder {
    private static final String NAME_TEMPLATE = "skaha-%s-ingress-%s";
    private static final String PATH_TEMPLATE = "/session/%s/%s/";
    private static final String SERVICE_NAME_TEMPLATE = "session-%s-svc-%s";
    private static final String PATH_MATCH_TYPE = "Prefix";
    private static final String CLASSNAME_ANNOTATIONS_KEY = "spec.ingressClassName";

    private String sessionID;
    private String namespace;
    private String type;
    private String ingressClassName;

    private SessionIngressBuilder() {

    }


    /**
     * Create a new SessionIngressBuilder from a sessionID.
     * @param sessionID The ID of the session to use for path mapping.
     * @return  SessionIngressBuilder instance.  Never null.
     */
    static SessionIngressBuilder fromSessionID(final String sessionID) {
        if (!StringUtil.hasText(sessionID)) {
            throw new IllegalArgumentException("Session ID must be provided.");
        }

        final SessionIngressBuilder builder = new SessionIngressBuilder();
        builder.sessionID = sessionID;

        return builder;
    }

    SessionIngressBuilder withNamespace(final String namespace) {
        if (!StringUtil.hasText(namespace)) {
            throw new IllegalArgumentException("Namespace must be provided.");
        }

        this.namespace = namespace;
        return this;
    }

    SessionIngressBuilder withIngressClassName(final String ingressClassName) {
        if (!StringUtil.hasText(ingressClassName)) {
            throw new IllegalArgumentException("Ingress class name must be provided.");
        }

        this.ingressClassName = ingressClassName;
        return this;
    }

    SessionIngressBuilder withType(final String type) {
        if (!StringUtil.hasText(type))  {
            throw new IllegalArgumentException("Type must be provided.");
        }

        this.type = type;
        return this;
    }

    /**
     *apiVersion: traefik.containo.us/v1alpha1
     * kind: IngressRoute
     * metadata:
     *   name: skaha-notebook-ingress-${skaha.sessionid}
     *   annotations:
     *     traefik.ingress.kubernetes.io/router.entrypoints: web,websecure
     *     traefik.ingress.kubernetes.io/router.tls: "true"
     * spec:
     *   entryPoints:
     *     - web
     *     - websecure
     *   routes:
     *   - kind: Rule
     *     match: Host(`${skaha.hostname}`) && PathPrefix(`/session/notebook/${skaha.sessionid}/`)
     *     services:
     *     - kind: Service
     *       name: skaha-notebook-svc-${skaha.sessionid}
     *       port: 8888
     *       scheme: http
     *
     * @return  String YAML representation of the Ingress.
     */
    String build() {
        if (!StringUtil.hasText(this.sessionID)) {
            throw new IllegalStateException("Session ID must be provided.");
        } else if (!StringUtil.hasText(this.type)) {
            throw new IllegalStateException("Type must be provided.");
        } else if (!StringUtil.hasText(this.ingressClassName)) {
            throw new IllegalStateException("Ingress class name must be provided.");
        }

        final V1Ingress ingress = new V1Ingress();
        final V1IngressSpec spec = new V1IngressSpec();
        ingress.apiVersion("networking.k8s.io/v1").kind("Ingress");

        final V1ObjectMeta metadata = new V1ObjectMeta().name(String.format(SessionIngressBuilder.NAME_TEMPLATE, this.type, this.sessionID))
                                                        .namespace(this.namespace)
                                                        .annotations(Collections.singletonMap(
                                                            SessionIngressBuilder.CLASSNAME_ANNOTATIONS_KEY, this.ingressClassName));
        ingress.metadata(metadata);

        final V1IngressRule ingressRule = new V1IngressRule();
        final V1HTTPIngressRuleValue ingressRuleValue = new V1HTTPIngressRuleValue();
        ingressRuleValue.addPathsItem(new V1HTTPIngressPath()
            .path(String.format(SessionIngressBuilder.PATH_TEMPLATE, this.type, this.sessionID))
            .pathType(SessionIngressBuilder.PATH_MATCH_TYPE)
            .backend(new V1IngressBackend()
                .service(new V1IngressServiceBackend()
                    .name(String.format(SessionIngressBuilder.SERVICE_NAME_TEMPLATE, this.sessionID, this.type))
                    .port(new V1ServiceBackendPort().number(8888))
                )
            )
        );

        ingressRule.http(ingressRuleValue);
        spec.addRulesItem(ingressRule);
        ingress.spec(spec);

        return Yaml.dump(ingress);
    }
}
