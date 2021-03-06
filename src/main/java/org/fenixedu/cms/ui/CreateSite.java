/**
 * Copyright © 2014 Instituto Superior Técnico
 *
 * This file is part of FenixEdu CMS.
 *
 * FenixEdu CMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu CMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu CMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.cms.ui;

import com.google.common.base.Strings;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.spring.portal.BennuSpringController;
import org.fenixedu.cms.domain.*;
import org.fenixedu.commons.i18n.LocalizedString;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import java.util.Set;

import static java.util.Optional.ofNullable;

@BennuSpringController(AdminSites.class)
@RequestMapping("/cms/sites/new")
public class CreateSite {

    @RequestMapping(method = RequestMethod.POST)
    public RedirectView create(@RequestParam LocalizedString name,
            @RequestParam(required = false, defaultValue = "{}") LocalizedString description,
            @RequestParam(required = false) SiteBuilder builder, @RequestParam(required = false) String theme,
            @RequestParam(required = false, defaultValue = "false") boolean embedded,
            @RequestParam(required = false) Set<String> roles,
            @RequestParam(required = false) String folder, RedirectAttributes redirectAttributes) {
        if (name.isEmpty()) {
            redirectAttributes.addFlashAttribute("emptyName", true);
            return new RedirectView("/cms/sites/new", true);
        } else {
            createSite(Sanitization.strictSanitize(name), Sanitization.sanitize(description), builder,false, folder, embedded,
                theme, roles);
            return new RedirectView("/cms/sites/", true);
        }
    }

    @Atomic
    private void createSite(LocalizedString name, LocalizedString description, SiteBuilder builder, boolean published, String folder,
            boolean embedded, String themeType, Set<String> roles) {
        CmsSettings.getInstance().ensureCanManageSettings();
        if (builder !=null){
            builder.create(name, description);
        } else {
            Site site = new Site(name, description);
        
            ofNullable(folder).filter(t -> !Strings.isNullOrEmpty(t)).map(FenixFramework::getDomainObject).map(CMSFolder.class::cast)
                    .ifPresent(site::setFolder);
    
            site.setEmbedded(ofNullable(embedded).orElse(false));
            site.updateMenuFunctionality();
            site.setPublished(published);
    
            ofNullable(roles).ifPresent(rolesSet->
                    rolesSet.forEach(role -> new Role(FenixFramework.getDomainObject(role), site)));
    
            ofNullable(themeType).filter(t -> !Strings.isNullOrEmpty(t)).map(CMSTheme::forType).ifPresent(site::setTheme);
    
            SiteActivity.createdSite(site, Authenticate.getUser());
        }
    }

}
