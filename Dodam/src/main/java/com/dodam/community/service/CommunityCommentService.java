package com.dodam.community.service;

import java.util.Map;

public interface CommunityCommentService {
 Map<String, Object> updateContent(Long conum, String ccontent);
 Map<String, Object> deleteOrSoftDelete(Long conum);
}

