package dev.characteratlas.archipelago.catalog.infrastructure;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
interface SearchContentMapper {
    @Select("SELECT content_id, visible FROM search_content_override")
    List<VisibilityRow> load();

    @Update("LOCK TABLE search_content_override IN SHARE ROW EXCLUSIVE MODE")
    void lock();

    @Update("""
            INSERT INTO search_content_override (content_id, visible) VALUES (#{id}, #{visible})
            ON CONFLICT (content_id) DO UPDATE SET visible = EXCLUDED.visible
            """)
    void save(@Param("id") String id, @Param("visible") boolean visible);

    record VisibilityRow(String id, boolean visible) {}
}
