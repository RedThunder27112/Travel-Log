package com.example.mylogbook.util

import java.io.File
import java.util.Properties
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

object EmailSender {
    private const val SMTP_HOST = "smtp.1and1.com"
    private const val SMTP_PORT = 587
    private const val SMTP_STARTTLS = true

    fun sendEmailWithAttachment(
        fromEmail: String,
        password: String,
        toEmail: String,
        subject: String,
        body: String,
        attachment: File
    ) {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", SMTP_STARTTLS.toString())
            put("mail.smtp.host", SMTP_HOST)
            put("mail.smtp.port", SMTP_PORT.toString())
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(fromEmail, password)
            }
        })

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(fromEmail))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
            setSubject(subject)
        }

        val textPart = MimeBodyPart().apply {
            setText(body)
        }
        val attachmentPart = MimeBodyPart().apply {
            dataHandler = DataHandler(FileDataSource(attachment))
            fileName = attachment.name
        }
        val multipart = MimeMultipart().apply {
            addBodyPart(textPart)
            addBodyPart(attachmentPart)
        }
        message.setContent(multipart)

        try {
            Transport.send(message)
        } catch (e: MessagingException) {
            throw IllegalStateException("Email send failed: ${e.message}", e)
        }
    }
}
