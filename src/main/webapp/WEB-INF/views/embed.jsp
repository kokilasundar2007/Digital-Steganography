<%@ page contentType="text/html;charset=UTF-8" %>
<%@ include file="fragments/head.jspf" %>
<c:set var="active" value="embed"/>
<!DOCTYPE html>
<html>
<head>
  <title>Embed · Digital Steganography</title>
</head>
<body>
<div class="shell">

  <%@ include file="fragments/sidebar.jspf" %>

  <main class="main">

    <div class="top">
      <div>
        <h1>Hide a Secret Message</h1>
        <p>Embed your secret message into an image using encryption.</p>
      </div>
    </div>

    <%@ include file="fragments/flash.jspf" %>

    <form method="post" enctype="multipart/form-data"
          action="${pageContext.request.contextPath}/embed" data-validate>
      <div class="work">

        <!-- LEFT SIDE: inputs -->
        <div>

          <!-- 1. Upload image -->
          <div class="card" style="margin-bottom:1.2rem">
            <span class="step">1. Upload Image</span>
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
          </div>

          <!-- 2, 3, 4. Message and passwords -->
          <div class="card">
            <div class="field">
              <label>2. Secret Message
                <small id="count" style="float:right">0/5000</small>
              </label>
              <textarea id="message" name="message" maxlength="5000"
                        placeholder="Enter your secret message here..." required></textarea>
            </div>

            <small class="cap">Image Capacity: <span id="capacity">—</span></small>

            <div class="work" style="margin-top:1rem">
              <div class="field">
                <label>3. Encryption Password</label>
                <input style="padding-left:.9rem" type="password" name="password"
                       placeholder="Enter password..." required>
              </div>
              <div class="field">
                <label>4. Confirm Password</label>
                <input style="padding-left:.9rem" type="password" name="confirmPassword"
                       placeholder="Confirm password..." required>
              </div>
            </div>

            <div class="btns">
              <button class="btn">Encrypt &amp; Embed Message</button>
              <button type="button" class="btn btn-outline" data-reset>Reset</button>
            </div>
          </div>
        </div>

        <!-- RIGHT SIDE: result -->
        <div class="card">
          <span class="step">Result</span>
          <c:choose>
            <c:when test="${not empty downloadId}">
              <div class="preview">
                <img src="${pageContext.request.contextPath}/download?id=${downloadId}&inline=1"
                     alt="stego image">
              </div>
              <a class="btn btn-block" style="margin-top:1rem"
                 href="${pageContext.request.contextPath}/download?id=${downloadId}">⬇ Download Stego Image</a>
            </c:when>
            <c:otherwise>
              <div class="result">Your stego image will appear here after successful embedding.</div>
            </c:otherwise>
          </c:choose>
        </div>

      </div>
    </form>

  </main>
</div>
</body>
</html>