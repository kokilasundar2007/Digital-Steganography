<%@ page contentType="text/html;charset=UTF-8" %>
<%@ include file="fragments/head.jspf" %>
<!DOCTYPE html>
<html>
<head>
  <title>Register · Digital Steganography</title>
</head>
<body>
<div class="auth">

  <!-- Left side: branding -->
  <section class="auth-hero">
    <div class="logo"><span>🛡️</span><div>Digital<br>Steganography</div></div>
    <h2 style="margin-top:2rem;font-weight:500">Secure Today.<br>Private Tomorrow.</h2>
  </section>

  <!-- Right side: registration card -->
  <section class="auth-card-wrap">
    <div class="auth-card">
      <h1>Create Your Account</h1>
      <p>Join us to start hiding your secrets.</p>

      <%@ include file="fragments/flash.jspf" %>

      <form method="post" action="${pageContext.request.contextPath}/register" data-validate>
        <div class="field">
          <span class="ico">👤</span>
          <input name="fullName" placeholder="Full Name" required
                 value="<c:out value='${param.fullName}'/>">
        </div>

        <div class="field">
          <span class="ico">✉</span>
          <input type="email" name="email" placeholder="Email Address" required
                 value="<c:out value='${param.email}'/>">
        </div>

        <div class="field">
          <span class="ico">👤</span>
          <input name="username" placeholder="Username" required
                 value="<c:out value='${param.username}'/>">
        </div>

        <div class="field">
          <span class="ico">🔒</span>
          <input type="password" name="password" placeholder="Password" required>
          <button type="button" class="eye">👁</button>
        </div>

        <div class="field">
          <span class="ico">🔒</span>
          <input type="password" name="confirmPassword" placeholder="Confirm Password" required>
          <button type="button" class="eye">👁</button>
        </div>

        <button class="btn btn-block">Create Account</button>
      </form>

      <p class="auth-foot">Already have an account?
        <a href="${pageContext.request.contextPath}/login">Login</a>
      </p>
    </div>
  </section>

</div>
</body>
</html>