package com.fspl.medica_healthcare.templets;

import com.fspl.medica_healthcare.enums.AppointmentStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailTemplets {
	
	public String getRegistrationSuccessTemplate(String hospitalName, String altUsername, String altPassword, String role, Long id) {
	    return "<!DOCTYPE html>" +
	           "<html>" +
	           "<head>" +
	           "<style>" +
	           "body {" +
	           "    font-family: Arial, sans-serif;" +
	           "    margin: 0;" +
	           "    padding: 0;" +
	           "    background-color: #f4f4f9;" +
	           "}" +
	           ".container {" +
	           "    width: 100%;" +
	           "    max-width: 600px;" +
	           "    margin: 20px auto;" +
	           "    background: #ffffff;" +
	           "    border-radius: 10px;" +
	           "    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);" +
	           "    overflow: hidden;" +
	           "}" +
	           ".header {" +
	           "    background: linear-gradient(135deg, #42a5f5, #0d47a1);" +
	           "    color: #ffffff;" +
	           "    text-align: center;" +
	           "    padding: 20px;" +
	           "    font-size: 24px;" +
	           "}" +
	           ".body {" +
	           "    padding: 20px;" +
	           "    line-height: 1.6;" +
	           "    color: #333333;" +
	           "}" +
	           ".footer {" +
	           "    background-color: #f4f4f9;" +
	           "    color: #888888;" +
	           "    text-align: center;" +
	           "    padding: 10px;" +
	           "    font-size: 12px;" +
	           "}" +
	           "</style>" +
	           "</head>" +
	           "<body>" +
	           "<div class=\"container\">" +
	           "<div class=\"header\">" +
	           "Welcome to " + hospitalName +
	           "</div>" +
	           "<div class=\"body\">" +
	           "<p>Dear User,</p>" +
	           "<p>Your registration for the " + hospitalName + " platform has been successfully completed!</p>" +
	           "<p>Here are your secure login details:</p>" +
	           "<p><strong>Login ID:</strong> " + altUsername + "</p>" +
	           "<p><strong>Access Key:</strong> " + altPassword + "</p>" +
	           "<p><strong>Role:</strong> " + role + "</p>" +
	           "<p><strong>Associate ID:</strong> " + id + "</p>" +
	           "<p>Please use these details to log in and change your password as soon as possible for added security.</p>" +
	           "</div>" +
	           "<div class=\"footer\">" +
	           "&copy; 2024 " + hospitalName + ". All Rights Reserved." +
	           "</div>" +
	           "</div>" +
	           "</body>" +
	           "</html>";
	}

	
	public String getUserDeactivationTemplate(String username, String hospitalName) {
	    return "<!DOCTYPE html>" +
	           "<html>" +
	           "<head>" +
	           "<style>" +
	           "body {" +
	           "    font-family: Arial, sans-serif;" +
	           "    margin: 0;" +
	           "    padding: 0;" +
	           "    background-color: #f4f4f9;" +
	           "}" +
	           ".container {" +
	           "    width: 100%;" +
	           "    max-width: 600px;" +
	           "    margin: 20px auto;" +
	           "    background: #ffffff;" +
	           "    border-radius: 10px;" +
	           "    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);" +
	           "    overflow: hidden;" +
	           "}" +
	           ".header {" +
	           "    background: linear-gradient(135deg, #42a5f5, #0d47a1);" +
	           "    color: #ffffff;" +
	           "    text-align: center;" +
	           "    padding: 20px;" +
	           "    font-size: 24px;" +
	           "}" +
	           ".body {" +
	           "    padding: 20px;" +
	           "    line-height: 1.6;" +
	           "    color: #333333;" +
	           "}" +
	           ".footer {" +
	           "    background-color: #f4f4f9;" +
	           "    color: #888888;" +
	           "    text-align: center;" +
	           "    padding: 10px;" +
	           "    font-size: 12px;" +
	           "}" +	           "</style>" +
	           "</head>" +
	           "<body>" +
	           "<div class=\"container\">" +
	           "<div class=\"header\">" +
	           "Account Deactivated" +
	           "</div>" +
	           "<div class=\"body\">" +
	           "<p>Dear " + username + ",</p>" +
	           "<p>Your account has been marked as inactive.</p>" +
	           "<p>This could be due to one of the following reasons:</p>" +
	           "<ul>" +
	           "<li>You chose to deactivate your account.</li>" +
	           "<li>An administrator deactivated your account.</li>" +
	           "</ul>" +
	           "<p>If you believe this is an error, please contact support.</p>" +
	           "</div>" +
	           "<div class=\"footer\">" +
	           "&copy; 2024 " + hospitalName + ". All Rights Reserved." +
	           "</div>" +
	           "</div>" +
	           "</body>" +
	           "</html>";
	}

	public String getUserReactivationTemplate(String username, String hospitalName) {
	    return "<!DOCTYPE html>" +
	           "<html>" +
	           "<head>" +
	           "<style>" +
	           "body {" +
	           "    font-family: Arial, sans-serif;" +
	           "    margin: 0;" +
	           "    padding: 0;" +
	           "    background-color: #f4f4f9;" +
	           "}" +
	           ".container {" +
	           "    width: 100%;" +
	           "    max-width: 600px;" +
	           "    margin: 20px auto;" +
	           "    background: #ffffff;" +
	           "    border-radius: 10px;" +
	           "    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);" +
	           "    overflow: hidden;" +
	           "}" +
	           ".header {" +
	           "    background: linear-gradient(135deg, #42a5f5, #0d47a1);" +
	           "    color: #ffffff;" +
	           "    text-align: center;" +
	           "    padding: 20px;" +
	           "    font-size: 24px;" +
	           "}" +
	           ".body {" +
	           "    padding: 20px;" +
	           "    line-height: 1.6;" +
	           "    color: #333333;" +
	           "}" +
	           ".footer {" +
	           "    background-color: #f4f4f9;" +
	           "    color: #888888;" +
	           "    text-align: center;" +
	           "    padding: 10px;" +
	           "    font-size: 12px;" +
	           "}" +	           "</style>" +
	           "</head>" +
	           "<body>" +
	           "<div class=\"container\">" +
	           "<div class=\"header\">" +
	           "Account Reactivated" +
	           "</div>" +
	           "<div class=\"body\">" +
	           "<p>Dear " + username + ",</p>" +
	           "<p>We are pleased to inform you that your account has been reactivated.</p>" +
	           "<p>You can now log in and continue using the application.</p>" +
	           "<p>If you have any questions, feel free to reach out to our support team.</p>" +
	           "</div>" +
	           "<div class=\"footer\">" +
	           "&copy; 2024 " + hospitalName + ". All Rights Reserved." +
	           "</div>" +
	           "</div>" +
	           "</body>" +
	           "</html>";
	}

	public String getPatientRegisterationTemplete(String patientName){

		return "<html>" +
				"<body style='font-family: Arial, sans-serif; color: #333;'>" +
				"<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 8px;'>" +
				"<h2 style='color: #4CAF50;'>🎉 Welcome to Medica HealthCare, " + patientName + "!</h2>" +
				"<p style='font-size: 16px; line-height: 1.6;'>Congratulations! You've successfully registered with Medica HealthCare. We’re thrilled to have you as part of our community! 🌟</p>" +
				"<p style='font-size: 16px; line-height: 1.6;'>Your health and well-being are our top priority, and we are committed to providing you with exceptional care. 💚💪</p>" +
				"<p style='font-size: 16px; line-height: 1.6;'>We’ll keep you informed with any important updates, and we’re always here to assist you with anything you need. 😊📲</p>" +
				"<p style='font-size: 16px; line-height: 1.6;'>Thank you for choosing Medica HealthCare. We’re excited to be part of your healthcare journey! 🙏</p>" +
				"<br>" +
				"<p style='font-size: 16px;'>Warm regards,</p>" +
				"<p style='font-size: 16px;'>The Medica HealthCare Team 🏥</p>" +
				"<p style='font-size: 16px;'><strong>📧 For inquiries, contact us at support@medica.com</strong></p>" +
				"</div>" +
				"</body>" +
				"</html>";
	}



	public String getAppointmentConfirmationTemplete(String patientName, String doctorName, String hospitalName, Enum<AppointmentStatus> appointmentStatus, LocalDateTime appoinmentDateAndTime){
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		String date =  appoinmentDateAndTime.toLocalDate().format(dateFormatter);
		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
		String time= appoinmentDateAndTime.toLocalTime().format(timeFormatter);
		return
				"<p>🎉 <b>Mark Your Calendar – You’re Booked for Wellness!</b></p>" +
						"<p>Hey <b>" + patientName + "</b>,</p>" +
						"<p>You’re officially scheduled for a date with better health!</p>" +
						"<p>👨‍⚕️ <b>Doctor:</b> " + doctorName + "<br/>" +
						"🏥 <b>Hospital:</b> " + hospitalName + "<br/>" +
						"📅 <b>Date:</b> " + date + "<br/>" +
						"⏰ <b>Time:</b> " + time + "</p>" +
						"<p>💡 <i>“Invest in your health today, because you deserve nothing less than the best!”</i></p>" +
						"<p>✨ We’re excited to make your visit a comfortable and memorable one. Got questions? We’re all ears!</p>" +
						"<p>Stay awesome,<br><b>"+hospitalName+"</b></p>";


	}

	public String getAppointmentUpdateNotificationTemplete(String patientName, String doctorName, String hospitalName, Enum<AppointmentStatus> appointmentStatus, LocalDateTime appoinmentDateAndTime){
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		String date =  appoinmentDateAndTime.toLocalDate().format(dateFormatter);
		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
		String time= appoinmentDateAndTime.toLocalTime().format(timeFormatter);
		return
				"<p>🎉 <b>Hi <b>" + patientName + "</b>,</b></p>" +
						"<p>We wanted to let you know that your appointment has been successfully updated!</p>" +
						"<p>👨‍⚕️ <b>Doctor:</b> Dr. " + doctorName + "<br/>" +
						"🏥 <b>Hospital:</b> " + hospitalName + "<br/>" +
						"📅 <b>Updated Date:</b> " + date + "<br/>" +
						"⏰ <b>Time:</b> " + time + "</p>" +
						"<p>🔄 <i>Your health matters to us, and we're here to ensure you're always taken care of!</i></p>" +
						"<p>✨ If you have any questions or need further assistance, don’t hesitate to reach out. We're always here for you!</p>" +
						"<p>Thank you for choosing Medica Healthcare. We can’t wait to see you!</p>" +
						"<p>Stay awesome,<br><b>" + hospitalName + "</b></p>";


	}

	public String getAppointmentCancellationTemplete(String patientName, String doctorName, String hospitalName, Enum<AppointmentStatus> appointmentStatus, LocalDateTime appoinmentDateAndTime){
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		String date =  appoinmentDateAndTime.toLocalDate().format(dateFormatter);
		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
		String time= appoinmentDateAndTime.toLocalTime().format(timeFormatter);
		return
				"<p>🚨 <b>Hi <b>" + patientName + "</b>,</b></p>" +
						"<p>We wanted to inform you that your appointment has been cancelled.</p>" +
						"<p>👨‍⚕️ <b>Doctor:</b> Dr. " + doctorName + "<br/>" +
						"🏥 <b>Hospital:</b> " + hospitalName + "<br/>" +
						"📅 <b>Date:</b> " + date + "<br/>" +
						"⏰ <b>Time:</b> " + time + "</p>" +
						"<p>🔄 <i>We sincerely apologize for any inconvenience caused. Your health is our top priority, and we’re here to help you reschedule at your convenience.</i></p>" +
						"<p>✨ If you have any questions or need further assistance, don’t hesitate to contact us. We’re always here to support you!</p>" +
						"<p>Thank you for understanding. We look forward to assisting you again soon!</p>" +
						"<p>Stay well,<br><b>" + hospitalName + "</b></p>";


	}

	public String getAppointmentMissingTemplate(String patientName, String doctorName, String hospitalName, Enum<AppointmentStatus> appointmentStatus, LocalDateTime appoinmentDateAndTime){
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		String date =  appoinmentDateAndTime.toLocalDate().format(dateFormatter);
		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
		String time= appoinmentDateAndTime.toLocalTime().format(timeFormatter);
		return
				"<p>🚨 <b>Hi <b>" + patientName + "</b>,</b></p>" +
						"<p>We noticed that you <b>" + appointmentStatus + "</b> your appointment.</p>" +
						"<p>👨‍⚕️ <b>Doctor:</b> Dr. " + doctorName + "<br/>" +
						"🏥 <b>Hospital:</b> " + hospitalName + "<br/>" +
						"📅 <b>Original Date:</b> " + date + "<br/>" +
						"⏰ <b>Time:</b> " + time + "</p>" +
						"<p>🔄 <i>Your health is important to us, and we kindly ask you to reschedule your appointment at the earliest convenience. Our team is here to assist you in finding a suitable date and time.</i></p>" +
						"<p>✨ If you have any questions or need assistance, please don’t hesitate to contact us. We’re always happy to help!</p>" +
						"<p>Thank you for your prompt attention. We look forward to serving you soon!</p>" +
						"<p>Stay well,<br><b>Medica Healthcare Team</b></p>";

	}

	public String getHospitalReactivationTemplate(String username, String hospitalName) {
		return "<!DOCTYPE html>" +
				"<html>" +
				"<head>" +
				"<style>" +
				"body {" +
				"    font-family: Arial, sans-serif;" +
				"    margin: 0;" +
				"    padding: 0;" +
				"    background-color: #f4f4f9;" +
				"}" +
				".container {" +
				"    width: 100%;" +
				"    max-width: 600px;" +
				"    margin: 20px auto;" +
				"    background: #ffffff;" +
				"    border-radius: 10px;" +
				"    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);" +
				"    overflow: hidden;" +
				"}" +
				".header {" +
				"    background: linear-gradient(135deg, #42a5f5, #0d47a1);" +
				"    color: #ffffff;" +
				"    text-align: center;" +
				"    padding: 20px;" +
				"    font-size: 24px;" +
				"}" +
				".body {" +
				"    padding: 20px;" +
				"    line-height: 1.6;" +
				"    color: #333333;" +
				"}" +
				".footer {" +
				"    background-color: #f4f4f9;" +
				"    color: #888888;" +
				"    text-align: center;" +
				"    padding: 10px;" +
				"    font-size: 12px;" +
				"}" +	           "</style>" +
				"</head>" +
				"<body>" +
				"<div class=\"container\">" +
				"<div class=\"header\">" +
				"Account Reactivated" +
				"</div>" +
				"<div class=\"body\">" +
				"<p>Dear " + username+ ",</p>" +
				"<p>We are pleased to inform you that your account has been reactivated.</p>" +
				"<p>You can now log in and continue using the application.</p>" +
				"<p>If you have any questions, feel free to reach out to our support team.</p>" +
				"</div>" +
				"<div class=\"footer\">" +
				"&copy; 2024 " + hospitalName + ". All Rights Reserved." +
				"</div>" +
				"</div>" +
				"</body>" +
				"</html>";
	}



}
