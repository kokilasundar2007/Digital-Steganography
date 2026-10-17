<%@ page contentType="text/html;charset=UTF-8" %>
<%@ include file="fragments/head.jspf" %>
<c:set var="active" value="extract"/>
<!DOCTYPE html>
<html>
<head>
  <title>Extract · Digital Steganography</title>
</head>
<body>
<div class="shell">

  <%@ include file="fragments/sidebar.jspf" %>

  <main class="main">

    <div class="top">
      <div>
        <h1>Extract a Hidden Message</h1>
        <p>Reveal the hidden message from a stego image using your password.</p>
      </div>
    </div>

    <%@ include file="fragments/flash.jspf" %>

    <form method="post" enctype="multipart/form-data"
          action="${pageContext.request.contextPath}/extract" data-validate>
      <div class="work">

        <!-- LEFT SIDE: inputs -->
        <div class="card">
          <span class="step">1. Upload Stego Image</span>
          <div class="drop">
            <div class="big">☁</div>
            <p>Drag &amp; drop an image here<br>or click to browse</p>
            <input type="file" name="image" accept=".png,.bmp">
            <span class="btn btn-sm" style="margin-top:.8rem">Browse Image</span><br>
            <small>Supported formats: PNG, BMP</small>
          </div>

          <div id="preview" class="preview empty" style="margin-top:1rem">
            <img alt="preview">
            <small class="fname"></small>
          </div>

          <div class="field" style="margin-top:1rem">
            <label>2. Decryption Password</label>
            <input style="padding-left:.9rem" type="password" name="password"
                   placeholder="Enter password..." required>
            <button type="button" class="eye">👁</button>
          </div>

          <div class="btns">
            <button class="btn">Extract Message</button>
            <button type="button" class="btn btn-outline" data-reset>Reset</button>
          </div>
        </div>

        <!-- RIGHT SIDE: extracted message -->
        <div class="card">
          <span class="step">Extracted Message</span>
          <c:choose>
            <c:when test="${not empty extracted}">
              <div class="result ok"><c:out value="${extracted}"/></div>
            </c:when>
            <c:otherwise>
              <div class="result">🔒<br>The secret message will appear here after successful extraction.</div>
            </c:otherwise>
          </c:choose>
        </div>

      </div>
    </form>

  </main>
</div>
</body>
</html>