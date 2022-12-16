import java.util.Properties;

public class ThreadLocalProperties extends Properties {
    private final ThreadLocal<Properties> propiedadesLocales = new ThreadLocal<Properties>() {
        @Override
        protected Properties initialValue() {
            return new Properties();
        }
    };

    public ThreadLocalProperties(Properties propiedades) {
        super(propiedades);
    }

    @Override
    public String getProperty(String key) {
        String valorLocal = propiedadesLocales.get().getProperty(key);
        return valorLocal == null ? super.getProperty(key) : valorLocal;
    }

    @Override
    public Object setProperty(String key, String value) {
        return propiedadesLocales.get().setProperty(key, value);
    }
}