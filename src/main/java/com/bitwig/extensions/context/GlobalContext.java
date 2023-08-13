package com.bitwig.extensions.context;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class GlobalContext {

    private static GlobalContext context;

    private Map<Class<?>, Object> crossExtensionProvider = new HashMap<>();
    private Map<Binder, Consumer<?>> registerCouplingListeners = new HashMap<>();
    private Map<Binder, Runnable> registerDeCouplingListeners = new HashMap<>();

    record Binder(Class<?> providerType, String listenerId) {
        // default record
    }

    private GlobalContext() {
    }

    public static GlobalContext getContext() {
        if (context == null) {
            context = new GlobalContext();
        }
        return context;
    }

    public <T> void removeListener(String listenerId) {
        crossExtensionProvider.values()
                .stream()
                .filter(CrossControlListenerHandler.class::isInstance)
                .map(CrossControlListenerHandler.class::cast)
                .forEach(provider -> provider.removeListener(listenerId));
        registerCouplingListeners.entrySet().removeIf(entry -> entry.getKey().listenerId.equals(listenerId));
        registerDeCouplingListeners.entrySet().removeIf(entry -> entry.getKey().listenerId.equals(listenerId));
    }

    public <T> void registerForListener(Class<T> providerType, String listenerId, Consumer<T> registerListener,
                                        Runnable decouplingListener) {
        T existing = (T) crossExtensionProvider.get(providerType);
        Binder binder = new Binder(providerType, listenerId);
        if (existing != null) {
            registerListener.accept(existing);
        } else {
            registerCouplingListeners.put(binder, registerListener);
        }
        registerDeCouplingListeners.put(binder, decouplingListener);
    }

    /**
     * The Provider registers its instance here.
     *
     * @param type
     * @param provider
     * @param <T>
     */
    public <T> void provideListener(Class<T> type, T provider) {
        if (!crossExtensionProvider.containsKey(type)) {
            crossExtensionProvider.put(type, provider);
            registerCouplingListeners.entrySet()
                    .stream()
                    .filter(entry -> entry.getKey().providerType.equals(type))
                    .map(entry -> entry.getValue()) //
                    .forEach(coupler -> ((Consumer<T>) coupler).accept(provider));
        }
    }

    /**
     * In this case the Provider exits and unregisters. The Provider is removed, and the clients are notified that
     * the provider is gone. The client (listener) needs resort back to its default behaviour.
     *
     * @param type coupled Type
     * @param <T>
     */
    public <T> void removeProvider(Class<T> type) {
        if (crossExtensionProvider.containsKey(type)) {
            registerDeCouplingListeners.entrySet()
                    .stream()
                    .filter(entry -> entry.getKey().providerType.equals(type))
                    .map(entry -> entry.getValue())
                    .forEach(decoupleAction -> decoupleAction.run());
            crossExtensionProvider.remove(type);
        }
    }

}
