/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.isis.extensions.secman.api.user.dom.mixins;

import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.MemberSupport;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.value.Password;
import org.apache.isis.commons.internal.exceptions._Exceptions;
import org.apache.isis.extensions.secman.api.IsisModuleExtSecmanApi;
import org.apache.isis.extensions.secman.api.user.spi.PasswordEncryptionService;
import org.apache.isis.extensions.secman.api.user.dom.ApplicationUser;
import org.apache.isis.extensions.secman.api.user.dom.ApplicationUserRepository;
import org.apache.isis.extensions.secman.api.user.dom.mixins.ApplicationUser_updatePassword.DomainEvent;

import lombok.RequiredArgsConstructor;
import lombok.val;

@Action(
        domainEvent = DomainEvent.class,
        semantics = SemanticsOf.IDEMPOTENT
)
@ActionLayout(
        associateWith = "hasPassword",
        sequence = "10"
)
@RequiredArgsConstructor
public class ApplicationUser_updatePassword {

    public static class DomainEvent
            extends IsisModuleExtSecmanApi.ActionDomainEvent<ApplicationUser_updatePassword> {}

    @Inject private ApplicationUserRepository applicationUserRepository;
    @Inject private Optional<PasswordEncryptionService> passwordEncryptionService; // empty if no candidate is available

    private final ApplicationUser target;

    @MemberSupport
    public ApplicationUser act(
            final Password existingPassword,
            final Password newPassword,
            final Password repeatNewPassword) {

        applicationUserRepository.updatePassword(target, newPassword.getPassword());
        return target;
    }

    @MemberSupport
    public boolean hideAct() {
        return !applicationUserRepository.isPasswordFeatureEnabled(target);
    }

    @MemberSupport
    public String disableAct() {

        if(!target.isForSelfOrRunAsAdministrator()) {
            return "Can only update password for your own user account.";
        }
        if (!target.isHasPassword()) {
            return "Password must be reset by administrator.";
        }
        return null;
    }

    @MemberSupport
    public String validateAct(
            final Password existingPassword,
            final Password newPassword,
            final Password repeatNewPassword) {

        if(!applicationUserRepository.isPasswordFeatureEnabled(target)) {
            return "Password feature is not available for this User";
        }

        val encrypter = passwordEncryptionService.orElseThrow(_Exceptions::unexpectedCodeReach);

        val encryptedPassword = target.getEncryptedPassword();

        if(target.getEncryptedPassword() != null) {
            if (!encrypter.matches(existingPassword.getPassword(), encryptedPassword)) {
                return "Existing password is incorrect";
            }
        }

        if (!Objects.equals(newPassword, repeatNewPassword)) {
            return "Passwords do not match";
        }

        return null;
    }


}
