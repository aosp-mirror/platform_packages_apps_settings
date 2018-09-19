package uitests.src.com.android.settings.search;

import android.text.TextUtils;
import java.util.Objects;


/**
 * Data class for {@link com.android.settings.search.SettingsSearchResultRegressionTest}
 */
public class SearchData {
  public final String title;
  public final String key;

  public String getTitle() {
    return title;
  }

  public String getKey() {
    return key;
  }
  public static final String DELIM = ";";

  public static SearchData from(String searchDataString) {
    String[] split = searchDataString.trim().split(DELIM, -1);

    if (split.length != 2) {
      throw new IllegalArgumentException("Arg is invalid: " + searchDataString);
    }

    return new SearchData.Builder()
        .setTitle(split[0])
        .setKey(split[1])
        .build();
  }

  @Override
  public String toString() {
    return title + DELIM + key;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SearchData)) {
      return false;
    }

    SearchData other = (SearchData) obj;
    return TextUtils.equals(this.title, other.title)
        && TextUtils.equals(this.key, other.key);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, key);
  }

  private SearchData(
      SearchData.Builder builder) {
    this.title = builder.title;
    this.key = builder.key;
  }

  public static class Builder {
    protected String title = "";
    protected String key = "";

    public SearchData build() {
      return new SearchData(this);
    }

    public SearchData.Builder setTitle(String title) {
      this.title = title;
      return this;
    }

    public SearchData.Builder setKey(String key) {
      this.key = key;
      return this;
    }
  }
}
