package pl.net.bluesoft.rnd.pt.dict.global;

import org.springframework.beans.factory.annotation.Autowired;
import pl.net.bluesoft.rnd.processtool.dict.DictionaryItemExt;
import pl.net.bluesoft.rnd.processtool.web.controller.ControllerMethod;
import pl.net.bluesoft.rnd.processtool.web.controller.IOsgiWebController;
import pl.net.bluesoft.rnd.processtool.web.controller.OsgiController;
import pl.net.bluesoft.rnd.processtool.web.controller.OsgiWebRequest;
import pl.net.bluesoft.rnd.processtool.web.domain.GenericResultBean;
import pl.net.bluesoft.rnd.processtool.dict.DictionaryItem;
import pl.net.bluesoft.rnd.processtool.dict.IDictionaryFacade;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: MZU
 * Date: 14.01.14
 * Time: 13:11
 * To change this template use File | Settings | File Templates.
 */
@OsgiController(name="dictservice")
public class DictionaryService  implements IOsgiWebController {
    private static Logger logger = Logger.getLogger(DictionaryService.class.getName());

    @Autowired
    private IDictionaryFacade dictionaryFacade;

    @ControllerMethod(action="getAll")
    public GenericResultBean getAll(final OsgiWebRequest invocation)
    {
        GenericResultBean result = new GenericResultBean();


        String dictId = invocation.getRequest().getParameter("dictionaryId");
        if(dictId==null || dictId.length() <=0){
            result.addError("Dictionary","dictionaryId is empty");
            return result;
        }
        Locale locale =   invocation.getRequest().getLocale();
        String langCode = locale.getLanguage();
        logger.log(Level.ALL, "Getting for language" + langCode);

        String filter = invocation.getRequest().getParameter("filter");

        String searchTerm = invocation.getRequest().getParameter("q");

        Collection<DictionaryItem> dictionaryItems = dictionaryFacade.getAllDictionaryItems(dictId, locale, filter);
        Collection<DictionaryItem> results = new ArrayList<DictionaryItem>();
        if(searchTerm == null || searchTerm.isEmpty())
        {
            results = dictionaryItems;
        }
        else
        {
            for(DictionaryItem item: dictionaryItems)

                if(isContainText(item, searchTerm.toLowerCase()))
                    results.add(item);
        }

        result.setData(results);

        return result;
    }


    private boolean isContainText(DictionaryItem item, String searchTerm)
    {
        if(item.getValue().toLowerCase().contains(searchTerm) || item.getKey().contains(searchTerm))
            return true;

        if(item.getDescription() != null && item.getDescription().toLowerCase().contains(searchTerm))
            return true;

        for(DictionaryItemExt ext: item.getExtensions()) {
			String temp = ext.getValue();
			String temp2 = item.getDescription();
			String st = temp+" - "+temp2;
			if ((st.replaceAll("\\s","")).toLowerCase().contains(searchTerm.replaceAll("\\s","")) || ext.getKey().contains(searchTerm))
				return true;
		}

        return false;
    }


}
