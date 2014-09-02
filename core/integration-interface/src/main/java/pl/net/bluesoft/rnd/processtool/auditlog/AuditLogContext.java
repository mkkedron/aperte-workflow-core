package pl.net.bluesoft.rnd.processtool.auditlog;

import org.codehaus.jackson.map.ObjectMapper;
import pl.net.bluesoft.rnd.processtool.auditlog.builders.AuditLogBuilder;
import pl.net.bluesoft.rnd.processtool.auditlog.builders.DefaultAuditLogBuilder;
import pl.net.bluesoft.rnd.processtool.auditlog.definition.AuditLogDefinition;
import pl.net.bluesoft.rnd.processtool.auditlog.model.AuditLog;
import pl.net.bluesoft.rnd.processtool.model.IAttributesProvider;
import pl.net.bluesoft.rnd.processtool.ui.widgets.HandlingResult;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static pl.net.bluesoft.rnd.processtool.plugins.ProcessToolRegistry.Util.getRegistry;

/**
 * User: POlszewski
 * Date: 2014-06-13
 */
public class AuditLogContext {
	private static Logger logger = Logger.getLogger(AuditLogContext.class.getName());
	private static final ThreadLocal<AuditLogBuilder> auditLogBuilder = new ThreadLocal<AuditLogBuilder>();

	public interface Callback {
		void invoke() throws Exception;
	}

	public static AuditLogBuilder get() {
		AuditLogBuilder result = auditLogBuilder.get();
		return result != null ? result : AuditLogBuilder.NULL;
	}

	public static Collection<HandlingResult> withContext(IAttributesProvider provider, Callback callback) {
		setup(provider);

		try {
			try {
				callback.invoke();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
			return get().getHandlingResults();
		}
		finally {
			cleanUp();
		}
	}

	private static void setup(IAttributesProvider provider)
    {
		auditLogBuilder.set(new DefaultAuditLogBuilder(provider));
	}




	private static void cleanUp() {
		auditLogBuilder.remove();
	}
}
