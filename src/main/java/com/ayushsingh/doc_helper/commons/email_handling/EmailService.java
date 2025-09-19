package com.ayushsingh.doc_helper.commons.email_handling;

public interface EmailService {

    public void sendEmail(String to, String subject, String body, boolean isHtml);
}
