/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.automation.javarules.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.AbstractScriptEngineFactory;
import org.openhab.core.automation.module.script.ScriptEngineFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.obermuhlner.scriptengine.java.JavaScriptEngine;
import ch.obermuhlner.scriptengine.java.JavaScriptEngineFactory;

/**
 * This is an implementation of a {@link ScriptEngineFactory} for Java, based on
 * https://github.com/eobermuhlner/java-scriptengine/
 * by Eric Obermühlner
 *
 * @author Jürgen Weber - Initial contribution
 */
@Component(service = ScriptEngineFactory.class)
@NonNullByDefault
public class JavaRuleEngineFactory extends AbstractScriptEngineFactory {

    private static final Logger logger = LoggerFactory.getLogger(JavaRuleEngineFactory.class);

    @Nullable
    BundleWiring bundleWiring;

    @Nullable
    private JavaScriptEngineFactory javaScriptEngineFactory;

    @Nullable
    private PackageResourceLister packageLister;

    @Activate
    protected void activate(BundleContext context, Map<String, ?> config) {

        packageLister = new PackageResourceLister() {

            @Override
            public Collection<String> listResources(String packageName) {
                return listClassResources(packageName);
            }
        };

        javaScriptEngineFactory = new JavaScriptEngineFactory();

        bundleWiring = context.getBundle().adapt(BundleWiring.class);

        logger.info("Bundle activated");
    }

    @Override
    public List<String> getScriptTypes() {
        String[] types = { "java" };
        return Arrays.asList(types);
    }

    @Override
    public @Nullable ScriptEngine createScriptEngine(String scriptType) {
        if (getScriptTypes().contains(scriptType)) {

            JavaScriptEngine engine = (JavaScriptEngine) javaScriptEngineFactory.getScriptEngine();

            engine.setExecutionStrategyFactory(new EntryExecutionStrategyFactory());
            engine.setPackageLister(packageLister);

            return engine;
        }
        return null;
    }

    // Compiler wants classes in used packages

    private Collection<String> listClassResources(String packageName) {

        String path = packageName.replace(".", "/");
        path = "/" + path;

        Collection<String> resources = bundleWiring.listResources(path, "*.class", 0);

        return resources;
    }
}
