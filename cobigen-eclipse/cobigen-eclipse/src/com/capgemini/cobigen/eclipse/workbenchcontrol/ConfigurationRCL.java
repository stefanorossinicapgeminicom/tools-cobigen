/*******************************************************************************
 * Copyright © Capgemini 2013. All rights reserved.
 ******************************************************************************/
package com.capgemini.cobigen.eclipse.workbenchcontrol;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

import com.capgemini.cobigen.CobiGen;
import com.capgemini.cobigen.exceptions.InvalidConfigurationException;

/**
 * {@link IResourceChangeListener} for the generator configuration project
 * 
 * @author mbrunnli (10.04.2013)
 */
public class ConfigurationRCL implements IResourceChangeListener {

    /**
     * Generator configuration project
     */
    private IProject generatorConfProj;

    /**
     * context.xml of the given project
     */
    private IFile contextXmlFile;

    /**
     * logback.xml of the given project
     */
    private IFile logbackXmlFile;

    /**
     * current {@link CobiGen} instance to get updated on changes
     */
    private CobiGen generator;

    /**
     * Assigning logger to ConfigurationRCL
     */
    private final static Logger LOG = LoggerFactory.getLogger(ConfigurationRCL.class);

    /**
     * 
     * @param generatorConfProj
     * @param generator
     * @author mbrunnli (10.04.2013)
     */
    public ConfigurationRCL(IProject generatorConfProj, CobiGen generator) {

        this.generatorConfProj = generatorConfProj;
        this.generator = generator;
        contextXmlFile = generatorConfProj.getFile("context.xml");
        logbackXmlFile = generatorConfProj.getFile("logback.xml");
        try {
            loadLogbackConfiguration(logbackXmlFile.getRawLocation().toString());
        } catch (IOException | JoranException e) {
            LOG.error("Logback file could not be read or written.", e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @author mbrunnli (10.04.2013)
     */
    @Override
    public void resourceChanged(IResourceChangeEvent event) {

        IResourceDelta[] affectedProjects = event.getDelta().getAffectedChildren(IResourceDelta.CHANGED);
        for (IResourceDelta projDelta : affectedProjects) {
            if (projDelta.getResource().equals(generatorConfProj)) {
                IResourceDelta[] affectedChildren = projDelta.getAffectedChildren(IResourceDelta.CHANGED);
                for (IResourceDelta fileDelta : affectedChildren) {
                    if (fileDelta.getResource().equals(contextXmlFile)) {
                        try {
                            generator.reloadConfigurationFromFile();
                        } catch (IOException e) {
                            LOG.error("Could not read the context.xml.", e);
                        } catch (InvalidConfigurationException e) {
                            MessageDialog.openWarning(Display.getDefault().getActiveShell(), "Warning",
                                    "The context.xml of the generator configuration was changed into an invalid state.\n"
                                            + "The generator might not behave as intended:\n" + e.getMessage());
                            LOG.warn(
                                    "The context.xml of the generator configuration was changed into an invalid state. The generator might not behave as intended:\n",
                                    e);
                        }
                    } else if (fileDelta.getResource().equals(logbackXmlFile)) {
                        try {
                            loadLogbackConfiguration(logbackXmlFile.getRawLocation().toString());
                        } catch (IOException e) {
                            LOG.error("Unable to read config file", e);
                        } catch (JoranException e) {
                            MessageDialog.openWarning(Display.getDefault().getActiveShell(), "Warning",
                                    "The context.xml of the generator configuration was changed into an invalid state.\n"
                                            + "The generator might not behave as intended:\n" + e.getMessage());
                            LOG.error(
                                    "{}{}{}",
                                    "Warning",
                                    "The context.xml of the generator configuration was changed into an invalid state.\n",
                                    "The generator might not behave as intended:\n", e.getMessage());
                        }
                    }
                }
            }
        }
    }

    /**
     * (Re-)Loads Logback configuration
     * 
     * @param externalConfigFileLocation location of the new logback.xml
     * @throws IOException if the file could not be read or written
     * @throws JoranException if the file could not be handled by log4j
     * @author sbasnet (11.06.2014)
     */
    public void loadLogbackConfiguration(String externalConfigFileLocation) throws IOException, JoranException {

        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        File externalConfigFile = new File(externalConfigFileLocation);
        if (!externalConfigFile.exists()) {
            throw new IOException("Logback External Config File Parameter does not reference a file that exists");
        } else {
            if (!externalConfigFile.isFile()) {
                throw new IOException("Logback External Config File Parameter exists, but does not reference a file");
            } else {
                if (!externalConfigFile.canRead()) {
                    throw new IOException("Logback External Config File exists and is a file, but cannot be read.");
                } else {
                    JoranConfigurator configurator = new JoranConfigurator();
                    configurator.setContext(lc);
                    lc.reset();
                    configurator.doConfigure(externalConfigFileLocation);

                    LOG.info("Configured Logback with config file from: " + externalConfigFileLocation);
                }
            }
        }
    }
}