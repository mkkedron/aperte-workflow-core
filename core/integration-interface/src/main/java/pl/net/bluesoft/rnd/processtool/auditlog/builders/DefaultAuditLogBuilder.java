package pl.net.bluesoft.rnd.processtool.auditlog.builders;

import pl.net.bluesoft.rnd.processtool.auditlog.definition.*;
import pl.net.bluesoft.rnd.processtool.auditlog.model.AuditLog;
import pl.net.bluesoft.rnd.processtool.auditlog.model.AuditedProperty;
import pl.net.bluesoft.rnd.processtool.model.AbstractPersistentEntity;
import pl.net.bluesoft.rnd.processtool.model.IAttributesProvider;
import pl.net.bluesoft.rnd.processtool.ui.widgets.HandlingResult;
import pl.net.bluesoft.util.lang.Pair;

import java.util.*;

import static pl.net.bluesoft.util.lang.Strings.hasText;

/**
 * User: POlszewski
 * Date: 2014-06-13
 */
public class DefaultAuditLogBuilder implements AuditLogBuilder {

	private final IAttributesProvider provider;

	private final Map<Pair<String, Object>, AuditLog> map = new HashMap<Pair<String, Object>, AuditLog>();
	private final Collection<HandlingResult> handlingResults = new ArrayList<HandlingResult>();


	public DefaultAuditLogBuilder(IAttributesProvider provider) {

		this.provider = provider;
	}


    @Override
    public boolean isNull() {
        return false;
    }

    @Override
	public void addSimple(String key, String oldValue, String newValue)
    {
        if(oldValue != null && !oldValue.equals(newValue)) {
            handlingResults.add(new HandlingResult(new Date(), key, true, oldValue, newValue));
        }
	}

    @Override
    public Collection<HandlingResult> getHandlingResults() {
        return handlingResults;
    }

}
