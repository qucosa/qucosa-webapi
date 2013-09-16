<%--
  ~ Copyright (C) 2013 SLUB Dresden
  ~
  ~ This program is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as published by
  ~ the Free Software Foundation, either version 3 of the License, or
  ~ (at your option) any later version.
  ~
  ~ This program is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~ GNU General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with this program.  If not, see <http://www.gnu.org/licenses/>.
  --%>

<html>
<head>
    <title>Qucosa Webapi</title>
</head>

<body>
<h1>Qucosa Webapi</h1>

<h2>General information</h2>

<ul>
    <li>The webapi module works on HTTP requests like GET, POST, UPDATE or DELETE (there are some more).</li>
    <li>The GET request could be handled by web browsers over links. All other kinds of requests could only performed
        with a REST client.
    </li>
</ul>

<p/>

<h2>Document:</h2>

<h3>get usage (works with web browsers and rest clients):</h3>
<ul>
    <li>
        <a href="document">GET /webapi/document</a><br/>get all documents in this repository
    </li>
    <li>
        <a href="document/37">GET /webapi/document/37</a><br/>get a specific document<br/>
        <strong>works only with test data</strong>
    </li>
</ul>

<p/>

<h3>post usage (only with rest client):</h3>
<ul>
    <li>TODO</li>
</ul>

<p/>

<h3>put usage (only with rest client):</h3>
<ul>
    <li>TODO</li>
</ul>

<p/>

<h3>delete usage (only with rest client):</h3>
<ul>
    <li>DELETE /webapi/document/37<br/>delete a specific document<br/>
        <strong>works only with test data</strong></li>
</ul>

<p/>

<h2>Searching:</h2>

<h3>get usage:</h3>

<p>
    Supported parameters:
<ul>
    <li>field&lt;number&gt;</li>
    <li>query&lt;number&gt;</li>
    <li>boolean&lt;number&gt;</li>
    <li>language</li>
    <li>searchtype</li>
</ul>
</p>

<p>
    Valid values for parameter <strong>field&lt;number&gt;</strong>:
<ul>
    <li>author</li>
    <li>title</li>
    <li>... supported values for lucence searching ...</li>
</ul>
</p>

<p/>

<p>
    Valid values for parameter <strong>query&lt;number&gt;</strong>:
<ul>
    <li>any utf-8 encoded input value</li>
</ul>
</p>

<p/>

<p>
    Valid values for paraemter <strong>boolean&lt;number&gt;</strong>:
<ul>
    <li>AND</li>
    <li>OR</li>
    <li>NOT ( &lt;expression&gt; and not &lt;expression&gt;)</li>
</ul>
</p>

<p/>

<p>&lt;number&gt; are numbers from 0 to infinity</p>


<p/>

<p>
    Valid values for parameter <strong>language</strong>strong:
<ul>
    <li>Zend_Language values or what ever lucence searching used ;)</li>
</ul>

<p/>

<p>
    Valid values for parameter <strong>searchtype</strong>:
<ul>
    <li>empty</li>
    <li>truncated - added to every search query a asteriks in front and end of term</li>
</ul>
</p>

<p/>

<p>Examples:
<ul>
    <li><a href="/webapi/search?field0=title&query0=gegen&searchtype=truncated">GET
        /webapi/search?field0=title&query0=gegen&searchtype=truncated</a></li>
    <li><a href="/webapi/search?field0=author&query0=oliver">GET /webapi/search?field0=author&query0=oliver</a></li>
</ul>
</p>

<p/>

<h3>post, put und delete usage:</h3>

<h4 style="color: red;">Will never be supported.</h4>

<p/>

</body>
</html>
