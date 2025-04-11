package rs.raf.email_service.utils;

import rs.raf.email_service.data.EmailType;

public class EmailUtils {
    public static String getEmailPlainContent(EmailType type, String token) {
        return switch (type) {
            case SET_PASSWORD -> "Set your password: http://localhost/" + token;
            case RESET_PASSWORD -> "Reset your password: http://localhost/" + token;
            case ACTIVATE_ACCOUNT -> "Activate your account: http://localhost/" + token;
            case REQUEST_CARD -> "Confirm that you sent this card request: http://localhost/" + token;
            case INSUFFICIENT_FUNDS -> "Please pay your loans: http://localhost/"+token;
        };
    }


    public static String getEmailSubject(EmailType type) {
        return switch (type) {
            case SET_PASSWORD -> "Set Your Password";
            case RESET_PASSWORD -> "Reset Your Password";
            case ACTIVATE_ACCOUNT -> "Activate Your Account";
            case REQUEST_CARD -> "Confirm card request";
            case INSUFFICIENT_FUNDS -> "Pay your loans";
        };
    }

    public static String getEmailContent(EmailType type, String token) {
        String title;
        String link;
        switch (type) {
            case SET_PASSWORD: {
                title = "Set Your Password";
                link = "http://localhost:4200/set-password/" + token;
                break;
            }
            case RESET_PASSWORD: {
                title = "Reset Your Password";
                link = "http://localhost:4200/reset-password/" + token;
                break;
            }
            case ACTIVATE_ACCOUNT: {
                title = "Activate Your Account";
                link = "http://localhost:4200/set-password/" + token;
                break;
            }
            case REQUEST_CARD: {
                title = "Confirm that you sent this card request";
                link = "http://localhost:4200/request-card" + token;
            }
            case INSUFFICIENT_FUNDS:{
                title = "Please pay your loans.";
                link = "http://localhost:4200/insufficient-funds/" + token;
            }
            default: {
                title = "Email lol";
                link = "http://localhost";
            }
        }
        String html = """
                <html>
                       <head>
                           <meta charset="utf-8">
                           <style type="text/css">
                               body {
                                   margin: 0;
                                   padding: 0;
                                   min-width: 100%;
                                   font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                                   background-color: #f7fafc;
                               }
                               .container {
                                   max-width: 600px;
                                   margin: 20px auto;
                                   padding: 30px;
                                   background-color: #ffffff;
                                   border-radius: 10px;
                                   box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                               }
                               .note {
                                   color: #718096;
                                   font-size: 14px;
                                   line-height: 1.6;
                                   text-align: center;
                                   margin: 25px 0;
                               }
                                .button-container {
                               text-align: center;
                               margin: 25px 0;
                                }
                               .security-alert {
                                   color: #e53e3e;
                                   font-size: 12px;
                                   text-align: center;
                                   padding: 15px;
                                   border: 1px solid #fed7d7;
                                   border-radius: 6px;
                                   margin-top: 25px;
                                   background-color: #fff5f5;
                               }
                               .footer {
                                   text-align: center;
                                   color: #a0aec0;
                                   font-size: 12px;
                                   margin-top: 40px;
                               }
                               .button {
                                   display: inline-block;
                                   padding: 12px 25px;
                                   background-color: #3182ce;
                                   color: white !important;
                                   text-decoration: none;
                                   border-radius: 6px;
                                   font-size: 16px;
                                   text-align: center;
                                   margin-top: 20px;
                               }
                               a {
                                    color: #fefefe;
                                }
                
                               .button:hover {
                                   background-color: #2b6cb0;
                               }
                               @media screen and (max-width: 600px) {
                                   .container {
                                       margin: 10px;
                                       padding: 20px;
                                   }
                               }
                           </style>
                       </head>
                       <body>
                           <div class="container">
                               <h1 style="text-align: center; color: #2d3748; margin-bottom: 30px;">"""
                + title + """
                </h1>
                <div class="button-container">
                     <a href='
                """ + link + """
                                           ' class="button">Set your password</a>
                                           </div>
                                            <p class="note">
                                               This link will be valid for 24 hours.<br>
                                               Please do not share this link with anyone.
                                           </p>
                                           <div class="security-alert">
                                               ⚠️ Warning: Our employees will never ask you for your password.
                                           </div>
                                           <div class="footer">
                                               <p>If you did not request this link, ignore this email.</p>
                                               <p>© 2025 Banka-3. All rights reserved.</p>
                                           </div>
                                       </div>
                                   </body>
                                   </html>
                """;

        return html;
    }
}
