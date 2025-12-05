package com.ayushsingh.doc_helper.features.email;

public interface EmailService {

    public void sendEmail(String to, String subject, String body, boolean isHtml);
}
