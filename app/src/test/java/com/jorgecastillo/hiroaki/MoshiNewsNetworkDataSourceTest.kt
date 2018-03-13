package com.jorgecastillo.hiroaki

import com.jorgecastillo.hiroaki.data.datasource.MoshiNewsNetworkDataSource
import com.jorgecastillo.hiroaki.data.networkdto.MoshiArticleDto
import com.jorgecastillo.hiroaki.data.service.MoshiNewsApiService
import com.jorgecastillo.hiroaki.model.Article
import com.jorgecastillo.hiroaki.models.fileBody
import com.jorgecastillo.hiroaki.models.inlineBody
import com.jorgecastillo.hiroaki.mother.anyArticle
import kotlinx.coroutines.experimental.runBlocking
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException

@RunWith(MockitoJUnitRunner::class)
class MoshiNewsNetworkDataSourceTest {

    private lateinit var dataSource: MoshiNewsNetworkDataSource
    private lateinit var server: MockWebServer

    @Before
    fun setup() {
        server = MockWebServer()
        dataSource = MoshiNewsNetworkDataSource(server.retrofitService(
                MoshiNewsApiService::class.java,
                MoshiConverterFactory.create()))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun sendsGetNews() {
        server.enqueueSuccessResponse("GetNews.json")

        runBlocking { dataSource.getNews() }

        server.assertRequest(
                sentToPath = "v2/top-headlines",
                queryParams = params(
                        "sources" to "crypto-coins-news",
                        "apiKey" to "a7c816f57c004c49a21bd458e11e2807"),
                headers = headers(
                        "Cache-Control" to "max-age=640000"
                ),
                method = "GET")
    }

    @Test
    fun sendsPublishHeadline() {
        server.enqueueSuccessResponse()
        val article = anyArticle()

        runBlocking { dataSource.publishHeadline(article) }

        server.assertRequest(
                sentToPath = "v2/top-headlines",
                jsonBodyResFile = fileBody("PublishHeadline.json", MoshiArticleDto::class.java),
                method = "POST")
    }

    @Test
    fun sendsPublishHeadlineUsingInlineBody() {
        server.enqueueSuccessResponse()
        val article = anyArticle()

        runBlocking { dataSource.publishHeadline(article) }

        server.assertRequest(
                sentToPath = "v2/top-headlines",
                jsonBody = inlineBody("{\n" +
                        "  \"title\": \"Any Title\",\n" +
                        "  \"description\": \"Any description\",\n" +
                        "  \"url\": \"http://any.url\",\n" +
                        "  \"urlToImage\": \"http://any.url/any_image.png\",\n" +
                        "  \"publishedAt\": \"2018-03-10T14:09:00Z\",\n" +
                        "  \"source\": {\n" +
                        "    \"id\": \"AnyId\",\n" +
                        "    \"name\": \"ANYID\"\n" +
                        "  }\n" +
                        "}\n", MoshiArticleDto::class.java))
    }

    @Test(expected = IllegalArgumentException::class)
    fun throwsWhenYouPassBothBodyParams() {
        server.enqueueSuccessResponse()
        val article = anyArticle()

        runBlocking { dataSource.publishHeadline(article) }

        server.assertRequest(
                sentToPath = "v2/top-headlines",
                jsonBodyResFile = fileBody("PublishHeadline.json", MoshiArticleDto::class.java),
                jsonBody = inlineBody("{\"title\" = \"Any title\" }", MoshiArticleDto::class.java))
    }

    @Test
    fun parsesNewsProperly() {
        server.enqueueSuccessResponse("GetNews.json")

        val news = runBlocking { dataSource.getNews() }

        thenNewsAreParsed(news)
    }

    @Test(expected = IOException::class)
    fun throwsIOExceptionOnGetNewsErrorResponse() {
        server.enqueueErrorResponse()

        runBlocking { dataSource.getNews() }
    }

    private fun thenNewsAreParsed(news: List<Article>) {
        assertThat(news.size, `is`(3))
    }
}
