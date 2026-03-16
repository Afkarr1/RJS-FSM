package com.rjs.fsm.notification;

public interface WhatsAppService {

    /**
     * Send a WhatsApp message to a phone number.
     * @param phoneNumber target phone in E.164 format (e.g. +628123456789)
     * @param message text message to send
     * @return true if sent successfully
     */
    boolean sendMessage(String phoneNumber, String message);
}
