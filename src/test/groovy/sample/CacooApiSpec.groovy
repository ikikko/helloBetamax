package sample

import groovyx.net.http.RESTClient

import org.apache.commons.jxpath.JXPathContext
import org.apache.commons.jxpath.JXPathNotFoundException
import org.apache.http.impl.conn.ProxySelectorRoutePlanner
import org.junit.Rule

import spock.lang.Shared
import spock.lang.Specification
import betamax.Betamax
import betamax.MatchRule
import betamax.Recorder

class CacooApiSpec extends Specification {

	static final String CACOO_API_URL = 'http://cacoo.com/api/v1'

	// tapeに記録した内容を用いてテストを行うので、正常なAPI KEYを設定する必要はない（このまま実行可能）
	static final String API_KEY = 'XXXXXXXXXXXXXX'

	// Betamaxを使用して、通信内容をtapeに記録/読込できるようにする
	@Rule Recorder recorder = new Recorder()

	@Shared RESTClient http = new RESTClient()

	// Betamax用プロキシを通すように設定
	def setupSpec() {
		http.client.routePlanner = new ProxySelectorRoutePlanner(http.client.connectionManager.schemeRegistry, ProxySelector.default)
	}

	// -------------------------------------- テストコード

	// 保存先のtape名を指定, ホスト名/パスが一致した場合はtapeから読み込む（クエリは判定対象外）
	@Betamax(tape="create diagram", match=[
		MatchRule.host,
		MatchRule.path
	])
	def "図の新規作成"() {
		when:
		def response = http.get([
			uri : "$CACOO_API_URL/diagrams/create.json",
			query : [apiKey : API_KEY],
		]).data

		then:
		getLastComment(response) == null
	}

	// 保存先のtape名を指定, ホスト名/パスが一致した場合はtapeから読み込む（クエリは判定対象外）
	@Betamax(tape="post comment", match=[
		MatchRule.host,
		MatchRule.path
	])
	def "コメントの追加"() {
		when:
		def diagramId = 'yy49ZKdgO9luyOxc'
		def response = http.get([
					uri : "$CACOO_API_URL/diagrams/$diagramId/comments/post.json",
					query : [apiKey : API_KEY, content : "テストコメント : ${new Date()}"],
				]).data

		then:
		getLastComment(response).contains('テストコメント')
	}

	// -------------------------------------- ヘルパーメソッド

	/**
	 * JSONデータから最新のコメントを抽出します
	 */
	def getLastComment(def data) {
		def context = JXPathContext.newContext(data)

		try {
			context.getValue('//content')
		} catch (JXPathNotFoundException e) {
			null
		}
	}

}
