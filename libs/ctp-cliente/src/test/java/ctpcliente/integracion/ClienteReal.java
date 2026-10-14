package ctpcliente.integracion;

import ctpcliente.CtpCliente;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Inyecta un {@link CtpCliente} conectado a los servidores reales. Se crea una sola vez por ejecucion
 * y se comparte entre todas las pruebas; JUnit lo cierra al terminar.
 */
public final class ClienteReal implements ParameterResolver {

    private static final ExtensionContext.Namespace ESPACIO = ExtensionContext.Namespace.create(ClienteReal.class);

    @Override
    public boolean supportsParameter(ParameterContext parametro, ExtensionContext contexto) {
        return parametro.getParameter().getType() == CtpCliente.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parametro, ExtensionContext contexto) {
        return contexto.getRoot().getStore(ESPACIO)
                .computeIfAbsent(CtpCliente.class, clase -> CtpCliente.crear(), CtpCliente.class);
    }
}
