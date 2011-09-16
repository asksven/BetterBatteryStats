/*
 * Copyright (C) 2011 asksven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.asksven.betterbatterystats;

/**
 * @author sven
 *
 */

import java.util.List;
import com.google.gson.Gson;

public class KbReader
{
    String json = 
        "{"
            + "'title': 'Computing and Information systems',"
            + "'id' : 1,"
            + "'children' : 'true',"
            + "'groups' : [{"
                + "'title' : 'Level one CIS',"
                + "'id' : 2,"
                + "'children' : 'true',"
                + "'groups' : [{"
                    + "'title' : 'Intro To Computing and Internet',"
                    + "'id' : 3,"
                    + "'children': 'false',"
                    + "'groups':[]"
                + "}]" 
            + "}]"
        + "}";

    public void main(String... args) throws Exception {

        // Now do the magic.
        Data data = new Gson().fromJson(json, Data.class);

        // Show it.
        System.out.println(data);
    }

}

class Data
{
    private String title;
    private Long id;
    private Boolean children;
    private List<Data> groups;

    public String getTitle() { return title; }
    public Long getId() { return id; }
    public Boolean getChildren() { return children; }
    public List<Data> getGroups() { return groups; }

    public void setTitle(String title) { this.title = title; }
    public void setId(Long id) { this.id = id; }
    public void setChildren(Boolean children) { this.children = children; }
    public void setGroups(List<Data> groups) { this.groups = groups; }

    public String toString() {
        return String.format("title:%s,id:%d,children:%s,groups:%s", title, id, children, groups);
    }
}