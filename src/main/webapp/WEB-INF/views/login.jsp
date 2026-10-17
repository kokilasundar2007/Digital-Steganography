<%@ page contentType="text/html;charset=UTF-8" %>
<%@ include file="fragments/head.jspf" %>
<!DOCTYPE html>
<html>
<head>
  <title>Login · Digital Steganography</title>
</head>
<body>
<div class="auth">

  <!-- Left side: branding -->
  <section class="auth-hero">
    <div class="shield">🛡️</div>
    <h1>Digital<br><span class="grad">Steganography</span></h1>
    <p>Your secrets. Your images. Your privacy.</p>
    <div class="feats">
      <span>🔒 Secure Encryption</span>
      <span>🖼 Image Steganography</span>
      <span>🛡 Privacy First</span>
    </div>
  </section>

  <!-- Right side: login card -->
  <section class="auth-card-wrap">
    <div class="auth-card">
      <h1>Welcome Back!</h1>
      <p>Sign in to access your steganography workspace.</p>

      <%@ include file="fragments/flash.jspf" %>

      <form method="post" action="${pageContext.request.contextPath}/login">
        <div class="field">
          <span class="ico">👤</span>
          <input name="username" placeholder="Username or email" required
                 value="<c:out value='${param.username}'/>">
        </div>

        <div class="field">
          <span class="ico">🔒</span>
          <input type="password" name="password" placeholder="Password" required>
          <button type="button" class="eye">👁</button>
        </div>

        <div class="row">
          <label><input type="checkbox" name="remember"> Remember me</label>
          <a href="#">Forgot password?</a>
        </div>

        <button class="btn btn-block">Login</button>
      </form>

      <p class="auth-foot">Don't have an account?
        <a href="${pageContext.request.contextPath}/register">Create one</a>
      </p>
    </div>
  </section>

</div>
</body>
</html>