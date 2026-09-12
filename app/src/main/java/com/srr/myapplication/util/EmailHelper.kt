package com.srr.myapplication.util

import com.srr.myapplication.model.QuotationRequest
import com.srr.myapplication.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailHelper {
    private const val ADMIN_EMAIL = "ranjith888999@gmail.com"
    private const val APP_PASSWORD = "gkwv czws rgnb dsdy"

    suspend fun sendQuotationEmail(
        request: QuotationRequest,
        user: User,
        nearestTechnicians: List<Pair<User, Float>>
    ) = withContext(Dispatchers.IO) {
        val props = Properties().apply {
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.socketFactory.port", "465")
            put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
            put("mail.smtp.auth", "true")
            put("mail.smtp.port", "465")
        }

        val session = Session.getDefaultInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(ADMIN_EMAIL, APP_PASSWORD)
            }
        })

        try {
            val mm = MimeMessage(session)
            mm.setFrom(InternetAddress(ADMIN_EMAIL))
            mm.addRecipient(Message.RecipientType.TO, InternetAddress(ADMIN_EMAIL))
            mm.subject = "New Service Request: ${request.productName}"
            
            val techList = nearestTechnicians.joinToString("\n") { (tech, dist) ->
                "- ${tech.name} (${tech.phone}) - Distance: ${String.format("%.2f", dist)} km"
            }

            val body = """
                Hello Admin,
                
                A new service request has been raised.
                
                USER DETAILS:
                Name: ${user.name}
                Phone: ${user.phone}
                Email: ${user.email}
                Location: ${request.comments.ifEmpty { user.location }}
                
                REQUEST DETAILS:
                Product: ${request.productName}
                Service Type: ${request.serviceType}
                Comments: ${request.comments}
                Timestamp: ${Date(request.timestamp)}
                
                NEAREST 5 TECHNICIANS:
                $techList
                
                Please take necessary action.
                
                Regards,
                Service For Ever App
            """.trimIndent()

            mm.setText(body)
            Transport.send(mm)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
