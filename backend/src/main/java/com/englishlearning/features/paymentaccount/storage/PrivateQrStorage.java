package com.englishlearning.features.paymentaccount.storage;
public interface PrivateQrStorage {
    void put(String object,byte[] png);
    byte[] read(String object);
    void delete(String object);
}
