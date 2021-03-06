package com.playarcanum.configurautomaton;

import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.json.JsonFormat;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.yaml.YamlFormat;
import com.playarcanum.configurautomaton.configuration.Configuration;
import com.playarcanum.olympus.annotations.Singleton;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@SuppressWarnings("ALL")
@Singleton
public final class Configurautomaton {
    private final ConcurrentHashMap<String, Supplier<?>> configurations;
    private final ConcurrentHashMap<String, String> paths;
    private final ObjectConverter objectConverter;
    private final Set<WeakReference<FileConfig>> openConfigs;

    public Configurautomaton() {
        this.configurations = new ConcurrentHashMap<>();
        this.paths = new ConcurrentHashMap<>();
        this.objectConverter = new ObjectConverter();
        this.openConfigs = new HashSet<>();
    }

    /**
     * Register an {@link IConfiguration} for later use.
     * @param configuration
     * @param <T>
     * @throws ConfigurautomatonException.AnnotationMissingException
     */
    public <T> void register(final @NonNull Class<T> configuration, final @NonNull Supplier<T> instance) throws ConfigurautomatonException.AnnotationMissingException{
        final Configuration annotation = configuration.getAnnotation(Configuration.class);
        if(annotation != null) {
            this.configurations.put(annotation.file(), instance);
        } else throw new ConfigurautomatonException.AnnotationMissingException(configuration);
    }

    /**
     * Register a configuration {@code path} with a corresponding {@code pathName}.
     * @param pathName
     * @param path
     */
    public void path(final @NonNull String pathName, final @NonNull String path) {
        this.paths.put(pathName, path);
    }

    /**
     * Create a {@link LoadedConfig} containing your specified configuratino object.
     * If writing changes to that configuration object, be sure to then pass the {@link LoadedConfig}
     * to {@link Configurautomaton#save(LoadedConfig)}.
     * @param pathName
     * @param fileName
     * @param <T>
     * @return the configuration file and the instantiated configuration object
     * @throws ConfigurautomatonException.PathException
     * @throws ConfigurautomatonException.ConfigurationException
     * @throws ConfigurautomatonException.LoadException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public <T> LoadedConfig<T> load(final @NonNull String pathName, final @NonNull String fileName)
            throws ConfigurautomatonException.PathException,
            ConfigurautomatonException.ConfigurationException,
            ConfigurautomatonException.LoadException,
            InstantiationException,
            IllegalAccessException,
            ConfigurautomatonException.FormatterException {
        final String path = this.paths.get(pathName);
        if(path == null) throw new ConfigurautomatonException.PathException(pathName);

        final Supplier<T> configuration = (Supplier<T>) this.configurations.get(fileName);
        if(configuration == null) throw new ConfigurautomatonException.ConfigurationException(fileName);

        final String file = path + "/" + fileName;
        final FileConfig config = this.loadFile(file);
        if(config == null) throw new ConfigurautomatonException.LoadException(file);

        final LoadedConfig<T> loadedConfig =  new LoadedConfig<T>(fileName, this.objectConverter.toObject(config, configuration), config);
        this.openConfigs.add(new WeakReference<>(loadedConfig.config));
        return loadedConfig;
    }

    /**
     * Create and return the {@link FileConfig} for the given path.
     * @param path
     * @return
     * @throws ConfigurautomatonException.FormatterException
     */
    private FileConfig loadFile(final @NonNull String path) throws ConfigurautomatonException.FormatterException {
        ConfigFormat<?> format;
        if(path.contains(".toml")) format = TomlFormat.instance();
        else if(path.contains(".yaml")) format = YamlFormat.defaultInstance();
        else if(path.contains(".json")) format = JsonFormat.fancyInstance();
        else throw new ConfigurautomatonException.FormatterException(path);

        final FileConfig config = FileConfig.builder(path, format)
                .autosave()
                //.concurrent()
                .build();
        if (config != null) {
            config.load();

            return config;
        } else return null;
    }

    /**
     * Simply saves the given configuration file.
     * @param config
     * @param <T>
     */
    public <T> void save(final @NonNull LoadedConfig<T> config) {
        this.objectConverter.toConfig(config.configuration, config.config);

        config.config.save();
        config.config.close();

        this.openConfigs.removeIf(reference -> reference.get() != null && reference.get().equals(config.config));
    }

    /**
     * A standard clean-up procedure that should be called when your program shuts down.
     * This saves and closes all open configurations.
     */
    public void shutdown() {
        final Iterator<WeakReference<FileConfig>> iterator = this.openConfigs.iterator();
        while(iterator.hasNext()) {
            final WeakReference<FileConfig> next = iterator.next();
            if(next.get() != null) {
                final FileConfig config = next.get();
                config.save();
                config.close();
            }
            iterator.remove();
        }
    }

    @AllArgsConstructor
    public static final class LoadedConfig<T> {
        private final String file;
        @Getter private final T configuration;
        private final FileConfig config;
    }
}
