package org.opencadc.skaha.session;

import ca.nrc.cadc.util.Log4jInit;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class SessionIngressBuilderTest {
    private static final Logger LOGGER = Logger.getLogger(SessionIngressBuilderTest.class);

    static {
        Log4jInit.setLevel(SessionIngressBuilderTest.class.getPackageName(), Level.DEBUG);
    }

    @Test
    public void testFromSessionID() {
        try {
            SessionIngressBuilder.fromSessionID(null);
            Assert.fail("Expected IllegalArgumentException not thrown.");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Session ID must be provided.", e.getMessage());
        }

        try {
            SessionIngressBuilder.fromSessionID("");
            Assert.fail("Expected IllegalArgumentException not thrown.");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Session ID must be provided.", e.getMessage());
        }

        final String sessionID = "88mph";
        final SessionIngressBuilder testSubject = SessionIngressBuilder.fromSessionID(sessionID);

        try {
            testSubject.build();
            Assert.fail("Expected IllegalStateException not thrown.");
        } catch (IllegalStateException e) {
            Assert.assertEquals("Type must be provided.", e.getMessage());
        }

        testSubject.withType("notebook");

        try {
            testSubject.build();
            Assert.fail("Expected IllegalStateException not thrown.");
        } catch (IllegalStateException e) {
            Assert.assertEquals("Ingress class name must be provided.", e.getMessage());
        }
    }

    @Test
    public void testWithNamespace() {
        final String sessionID = "88mph";
        final SessionIngressBuilder testSubject = SessionIngressBuilder.fromSessionID(sessionID)
                                                                       .withType("notebook")
                                                                       .withIngressClassName("traefik");

        try {
            testSubject.withNamespace(null);
            Assert.fail("Expected IllegalArgumentException not thrown.");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Namespace must be provided.", e.getMessage());
        }

        try {
            testSubject.withNamespace("");
            Assert.fail("Expected IllegalArgumentException not thrown.");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Namespace must be provided.", e.getMessage());
        }
    }

    @Test
    public void testWithType() {
        final String sessionID = "88mph";
        final SessionIngressBuilder testSubject = SessionIngressBuilder.fromSessionID(sessionID)
                                                                       .withNamespace("skaha")
                                                                       .withIngressClassName("traefik");

        try {
            testSubject.withType(null);
            Assert.fail("Expected IllegalArgumentException not thrown.");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Type must be provided.", e.getMessage());
        }

        try {
            testSubject.withType("");
            Assert.fail("Expected IllegalArgumentException not thrown.");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Type must be provided.", e.getMessage());
        }
    }

    @Test
    public void testWithIngressClassName() {
        final String sessionID = "88mph";
        final SessionIngressBuilder testSubject = SessionIngressBuilder.fromSessionID(sessionID)
                                                                       .withNamespace("skaha")
                                                                       .withType("notebook");

        try {
            testSubject.withIngressClassName(null);
            Assert.fail("Expected IllegalArgumentException not thrown.");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Ingress class name must be provided.", e.getMessage());
        }

        try {
            testSubject.withIngressClassName("");
            Assert.fail("Expected IllegalArgumentException not thrown.");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Ingress class name must be provided.", e.getMessage());
        }
    }

    @Test
    public void testOutput() {
        final String sessionID = "88mph";
        final String namespace = "skaha";
        final String type = "notebook";
        final String ingressClassName = "traefik";

        final SessionIngressBuilder testSubject = SessionIngressBuilder.fromSessionID(sessionID)
                                                                       .withNamespace(namespace)
                                                                       .withType(type)
                                                                       .withIngressClassName(ingressClassName);

        final String output = testSubject.build();
        LOGGER.info("Output: " + output);

        Assert.assertTrue(output.contains("apiVersion: networking.k8s.io/v1"));
        Assert.assertTrue(output.contains("kind: Ingress"));
        Assert.assertTrue(output.contains("name: skaha-notebook-ingress-88mph"));
        Assert.assertTrue(output.contains("namespace: skaha"));
        Assert.assertTrue(output.contains("traefik"));
        Assert.assertTrue(output.contains("notebook"));
    }
}
