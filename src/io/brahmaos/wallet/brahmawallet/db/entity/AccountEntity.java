/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.brahmaos.wallet.brahmawallet.db.entity;

import java.io.Serializable;
import java.util.List;

import io.brahmaos.wallet.brahmawallet.model.Account;

public class AccountEntity implements Account, Serializable {
    private int id;
    private String name;
    private String address;
    private String filename;
    private int type;
    private String cryptoMnemonics;
    private boolean isDefault;
    List<String> mnemonics;

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public List<String> getMnemonics() {
        return mnemonics;
    }

    public void setMnemonics(List<String> mnemonics) {
        this.mnemonics = mnemonics;
    }

    public AccountEntity() {
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getCryptoMnemonics() {
        return cryptoMnemonics;
    }

    public void setCryptoMnemonics(String cryptoMnemonics) {
        this.cryptoMnemonics = cryptoMnemonics;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public AccountEntity(int id, String name, String address, String filename,
                         int type, String cryptoMnemonics, boolean isDefault) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.filename = filename;
        this.type = type;
        this.cryptoMnemonics = cryptoMnemonics;
        this.isDefault = isDefault;
    }

    public AccountEntity(Account account) {
        this.id = account.getId();
        this.name = account.getName();
        this.address = account.getAddress();
        this.filename = account.getFilename();
        this.type = account.getType();
        this.cryptoMnemonics = account.getCryptoMnemonics();
        this.isDefault = account.isDefault();
    }

    @Override
    public String toString() {
        return "AccountEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", filename='" + filename + '\'' +
                ", type=" + type +
                ", cryptoMnemonics='" + cryptoMnemonics + '\'' +
                ", isDefault=" + isDefault +
                ", mnemonics=" + mnemonics +
                '}';
    }
}
