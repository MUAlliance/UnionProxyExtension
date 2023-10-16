package work.art1st.unionextension.common.unionAPI;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.util.List;

@Getter
public class BackendInfo {
    @Getter
    public static class BackendScopes {
        private String self;
        private List<String> all;
    }

    @SerializedName("backend_scopes")
    private BackendScopes backendScopes;
}
