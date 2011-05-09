import org.apache.solr.AbstractSolrTestCase
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer
import org.filirom1.SearchHelper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4)
class SearchTest extends AbstractSolrTestCase {
  SearchHelper search

  @Override
  public String getSchemaFile() {
    return "solr/conf/schema.xml";
  }

  @Override
  public String getSolrConfigFile() {
    return "solr/conf/solrconfig.xml";
  }

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    search = new SearchHelper();
    search.server = new EmbeddedSolrServer(h.getCoreContainer(), h.getCore().getName());
    search.list().collect {search.removeFromIndex(it)}
    search.updateIndex()
    assertEquals 0, search.list().size()
  }

  private File getResource(String name) {
    return new File(getClass().getClassLoader().getResource(name).file)
  }

  @Test
  void testAddToIndex() {
    //execute
    search.addToIndex("ID-1", [param_t: "param", param2_t: "param2"])

    //check
    assertEquals 1, search.list().size()
    assertEquals "ID-1", search.list()[0]
  }

  @Test
  void testAddToIndexWithoutParams() {
    //execute
    search.addToIndex("ID-1", [:])

    //check
    assertEquals 1, search.list().size()
  }

  @Test
  void testAddToIndexWithFile() {
    //execute
    search.addToIndex("ID-1", [param_t: "param", param2_t: "param2"], getResource('document.html'))

    //check
    assertEquals 1, search.list().size()
  }

  @Test
  void testAddToIndexButFileNotFound() {
    //execute
    search.addToIndex("ID-1", [param_t: "param", param2_t: "param2"], new File("not found"))

    //check
    assertEquals 1, search.list().size()
  }

  @Test
  void testAddToIndexButFileIsNull() {
    //execute
    search.addToIndex("ID-1", [param_t: "param", param2_t: "param2"], null)

    //check
    assertEquals 1, search.list().size()
  }

  @Test
  void testDelete() {
    //fixture
    def id = "ID-1"
    search.addToIndex(id, null, getResource("document.html"))
    assertEquals 1, search.list().size()

    //execute
    search.removeFromIndex(id);

    //check
    search.updateIndex()
    assertEquals 0, search.list().size()
  }

  @Test
  void testSearchOnContent() {
    //fixture
    search.addToIndex("ID-1", null, getResource("document.html"))
    search.addToIndex("ID-2", null, getResource("courgette.html"))

    //execute
    def list = search.search('"la poste"');

    //check
    assertEquals 1, list.size()
  }


  @Test
  void testSearchOnField() {
    //fixture
    search.addToIndex("ID-1", [param_t: "patate"], getResource("document.html"))
    search.addToIndex("ID-2", [param_t: "poireau"], getResource("document.html"))

    //execute
    def list = search.search('param_t: "poireau"');

    //check
    assertEquals 1, list.size()
  }

}
