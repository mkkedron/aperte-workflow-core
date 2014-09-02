package pl.net.bluesoft.rnd.processtool.auditlog.builders;

import pl.net.bluesoft.rnd.processtool.auditlog.model.AuditLog;
import pl.net.bluesoft.rnd.processtool.model.AbstractPersistentEntity;
import pl.net.bluesoft.rnd.processtool.ui.widgets.HandlingResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * User: POlszewski
 * Date: 2014-06-12
 */
public interface AuditLogBuilder {
	boolean isNull();
	void addSimple(String key, String oldValue, String newValue);
    Collection<HandlingResult> getHandlingResults();

	AuditLogBuilder NULL = new AuditLogBuilder() {

        @Override
        public boolean isNull() {
            return false;
        }

        @Override
        public void addSimple(String key, String oldValue, String newValue) {

        }

        @Override
        public Collection<HandlingResult> getHandlingResults() {
            return new ArrayList<HandlingResult>();
        }
    };
}
