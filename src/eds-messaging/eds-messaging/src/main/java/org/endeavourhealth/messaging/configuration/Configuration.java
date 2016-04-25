package org.endeavourhealth.messaging.configuration;

import org.endeavourhealth.messaging.exceptions.ReceiverNotFoundException;
import org.endeavourhealth.messaging.model.IReceivePortHandler;
import org.endeavourhealth.messaging.utilities.FileHelper;
import org.endeavourhealth.messaging.utilities.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import static org.endeavourhealth.messaging.configuration.PluginLoader.getPluginsPath;
import static org.endeavourhealth.messaging.configuration.PluginLoader.loadPlugins;

public class Configuration
{
    private static Configuration instance = null;

    public static Configuration getInstance() throws Exception
    {
        // TODO - thread safety

        if (instance == null)
            instance = new Configuration();

        return instance;
    }

    private List<Plugin> plugins;

    protected Configuration() throws Exception
    {
        loadConfiguration();
    }

    private void loadConfiguration() throws Exception
    {
        Log.info("Searching for plugins at " + PluginLoader.getPluginsPath());

        this.plugins = PluginLoader.loadPlugins();

        if (plugins.size() > 0)
            plugins.forEach(t -> Log.info("Loaded plugin " + t.getName()));
        else
            Log.info("No plugins found.");
    }

    public List<Plugin> getPlugins()
    {
        return plugins;
    }

    public IReceivePortHandler getReceivePortHandler(String method, String path) throws Exception
    {
//        for (Plugin plugin : plugins)
//        {
//            IReceivePortHandler receiver = plugin.getReceiver(method, path);
//
//            if (receiver != null)
//                return receiver;
//        }

        throw new ReceiverNotFoundException();
    }

//    public Boolean isContractValid(MessageIdentity messageIdentity) throws MessageNotFoundException
//    {
//        for (Plugin plugin : plugins)
//        {
//            IReceivePortHandler endpoint = plugin.isContractValid(messageIdentity);
//
//            if (endpoint != null)
//                return endpoint;
//        }
//
//        return null;
//    }
}
