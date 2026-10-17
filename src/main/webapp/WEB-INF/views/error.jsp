<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<%@ include file="fragments/head.jspf" %>
<!DOCTYPE html>
<html>
<head>
  <title>Error · Digital Steganography</title>
</head>
<body>
<div class="auth" style="grid-template-columns:1fr">
  <section class="auth-card-wrap">
    <div class="auth-card" style="text-align:center">
      <div style="font-size:3rem">⚠️</div>
      <h1>Something went wrong</h1>
      <p>The page you requested could not be shown. Please try again.</p>
      <a class="btn btn-block" href="${pageContext.request.contextPath}/login">Back to Home</a>
    </div>
  </section>
</div>
</body>
</html>