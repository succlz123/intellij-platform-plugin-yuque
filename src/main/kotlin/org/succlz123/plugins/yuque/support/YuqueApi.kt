package org.succlz123.plugins.yuque.support

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class YuqueHelper {
    val timeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.CHINA)

    val config = YuqueConfig()

    val api = YuqueApi(config.baseUrl)

    var yuqueUser: YuqueUser? = null
    var repoList: List<YuqueRepoList>? = null
    var articleListMap: HashMap<String, List<YuqueArticleList>> = hashMapOf()

    fun initToken(token: String) {
        api.token = token
    }

    fun fetchUserInfo(force: Boolean = false): YuqueUser? {
        if (!force && yuqueUser != null) {
            return yuqueUser
        }
        yuqueUser = api.userInfo()
        return yuqueUser
    }

    fun fetchUserRepo(login: String?, force: Boolean = false): List<YuqueRepoList>? {
        if (!force && !repoList.isNullOrEmpty()) {
            return repoList
        }
        repoList = api.repoList(login)
        return repoList
    }

    fun fetchArticles(id: String?, force: Boolean = false): List<YuqueArticleList>? {
        if (id.isNullOrEmpty()) {
            return null
        }
        var list = articleListMap[id]
        if (!force && !list.isNullOrEmpty()) {
            return list
        }
        list = api.articleList(id)
        if (list == null) {
            return null
        }
        articleListMap[id] = list
        return list
    }

    fun fetchArticleDetail(namespace: String?, article: YuqueArticleList): YuqueArticleDetail? {
        val docDetail = api.getArticleDetail(namespace, article.slug)
        val body = docDetail?.body
        if (!body.isNullOrEmpty()) {
            val format = format(body)
            docDetail.body = format
        }
        return docDetail
    }

    private fun format(body: String): String {
        val multiBr = "/(<br>[\\s\\n]){2}/gi"
        val multiBrEnd = "/(<br \\/>[\\n]?){2}/gi"
        val brBug = "/<br \\/>/g"
        val hiddenContent = "/<div style=\"display:none\">[\\s\\S]*?<\\/div>/gi"
        // 删除语雀特有的锚点
        val emptyAnchor = "/<a name=\\\".*?\\\"><\\/a>/g"
        val format = body
                .replace(hiddenContent, "")
                .replace(multiBr, "<br>")
                .replace(multiBrEnd, "<br />\\n")
                .replace(brBug, "\\n")
                .replace(emptyAnchor, "")
        return format
    }
}

data class YuqueConfig(
        var baseUrl: String = "https://www.yuque.com/api/v2"
)

class YuqueApi(var baseUrl: String) {
    var token: String = ""

    inline fun <reified T> genericType() = object : TypeToken<T>() {}.type

    fun userInfo(): YuqueUser? {
        val url = "$baseUrl/user"
        val json = getString(url)
        if (json.isNullOrEmpty()) {
            return null
        }
        val fromJson = Gson().fromJson<YuqueUserData>(json, genericType<YuqueUserData>())
        return fromJson.data
    }

    fun repoList(login: String?): List<YuqueRepoList>? {
        val url = "$baseUrl/users/$login/repos"
        val json = getString(url)
        if (json.isNullOrEmpty()) {
            return null
        }
        val fromJson = Gson().fromJson<YuqueRepoListData>(json, genericType<YuqueRepoListData>())
        return fromJson.data
    }

    fun articleList(id: String?): List<YuqueArticleList>? {
        val url = "$baseUrl/repos/$id/docs"
        val json = getString(url)
        if (json.isNullOrEmpty()) {
            return null
        }
        val fromJson = Gson().fromJson<YuqueArticleListData>(json, genericType<YuqueArticleListData>())
        return fromJson.data
    }

    fun getArticleDetail(namespace: String?, slug: String?): YuqueArticleDetail? {
        if (slug.isNullOrEmpty()) {
            return null
        }
        val url = "$baseUrl/repos/$namespace/docs/$slug/?raw=1"
        val json = getString(url)
        if (json.isNullOrEmpty()) {
            return null
        }
        val fromJson = Gson().fromJson<YuqueArticleDetailData>(json, genericType<YuqueArticleDetailData>())
        return fromJson.data
    }

    fun create() {}

    fun update() {
    }

    fun delete() {
    }

    fun getString(url: String): String? {
        var conn: HttpURLConnection? = null
        try {
            conn = url.openConnection(hashMapOf("X-Auth-Token" to token))
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("unexpected http code :$code")
            }
            val sb = StringBuilder()
            conn.inputStream.bufferedReader().useLines { lines ->
                lines.forEach {
                    sb.append(it)
                }
            }
            return sb.toString()
        } catch (e: Exception) {
        } finally {
            conn?.disconnect()
        }
        return null
    }

    @Throws(IOException::class)
    fun String.openConnection(
            header: Map<String, String>? = null,
            connectTimeout: Int = 6000,
            readTimeout: Int = 6000
    ): HttpURLConnection {
        val connection = URL(this).openConnection() as HttpURLConnection
        header?.forEach {
            connection.setRequestProperty(it.key, it.value)
        }
        connection.connectTimeout = connectTimeout
        connection.readTimeout = readTimeout
        return connection
    }
}

data class YuqueUserData(var `data`: YuqueUser? = null)

class YuqueUser {
    var id: Long? = null
    var type: String? = null
    var login: String? = null
    var name: String? = null
    var avatar_url: String? = null
    var description: String? = null

    var token: String? = null

    var _serializer: String? = null
    var created_at: String? = null
    var followers_count: Int = 0
    var following_count: Int = 0
    var updated_at: String? = null
}

data class YuqueRepoListData(var `data`: List<YuqueRepoList>? = null)

data class YuqueRepoList(var id: String? = null, var slug: String? = null, var name: String? = null, var namespace: String? = null) {
    override fun toString(): String {
        return name.orEmpty()
    }
}

data class YuqueArticleListData(
        var `data`: List<YuqueArticleList>? = null
)

class YuqueArticleList {
    var id: Long? = null
    var _serializer: String? = null
    var book_id: Int = 0
    var comments_count: Int = 0
    var content_updated_at: String? = null
    var cover: String? = null
    var custom_description: String? = null
    var title: String? = null
    var description: String? = null
    var draft_version: Int = 0
    var first_published_at: String? = null
    var format: String? = null
    var last_editor_id: Int = 0
    var last_editor_name: String? = null
    var last_editor_avatar_url: String? = null

    var likes_count: Int = 0
    var `public`: Int = 0
    var read_status: Int = 0
    var slug: String? = null
    var status: Int = 0
    var user_id: Int = 0
    var view_status: Int = 0
    var word_count: Int = 0

    var created_at: String? = null
    var updated_at: String? = null
    var published_at: String? = null

    override fun toString(): String {
        return title.orEmpty()
    }
}

class YuqueArticleDetailData {
    var `data`: YuqueArticleDetail? = null
}

class YuqueArticleDetail {
    var id: Long? = null
    var _serializer: String? = null
    var title: String? = null
    var slug: String? = null
    var body: String? = null
    var body_draft: String? = null
    var body_draft_lake: String? = null
    var body_html: String? = null
    var body_lake: String? = null
    var book_id: Int = 0 // this article belongs to the repo id
    var comments_count: Int = 0
    var cover: String? = null
    var description: String? = null
    var custom_description: String? = null
    var format: String? = null
    var user_id: Int = 0
    var user_name: String? = null
    var user_avatar_url: String? = null

    var `public`: Int = 0
    var hits: Int = 0
    var likes_count: Int = 0
    var status: Int = 0
    var read_status: Int = 0
    var view_status: Int = 0
    var word_count: Int = 0

    var content_updated_at: String? = null
    var first_published_at: String? = null
    var published_at: String? = null
    var created_at: String? = null
    var updated_at: String? = null
    var deleted_at: String? = null
}
