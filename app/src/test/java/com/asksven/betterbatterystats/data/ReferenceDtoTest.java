package com.asksven.betterbatterystats.data;

import com.asksven.android.common.dto.MiscDto;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertTrue;

/**
 * Created on 12/26/16.
 */
public class ReferenceDtoTest
{
	@Test
	public void test_marshall_empty() throws Exception
	{
		ReferenceDto dto = new ReferenceDto();

		byte[] dtoBytes = dto.marshall();
		assertTrue(dtoBytes.length > 0);
	}

	@Test
	public void test_unmarshall_empty() throws Exception
	{
		ReferenceDto dto = new ReferenceDto();

		byte[] dtoBytes = dto.marshall();
		assertTrue(dtoBytes.length > 0);

		ReferenceDto dto2 = ReferenceDto.unmarshall(dtoBytes);
		assertTrue(dto2 != null);
		assertTrue(dto2.m_fileName.equals(dto.m_fileName));
	}

	@Test
	public void test_marshall_populated() throws Exception
	{
		ReferenceDto dto = new ReferenceDto();
		dto.m_fileName = "filename";

		ArrayList<MiscDto> misc = new ArrayList<MiscDto>();
		MiscDto entry = new MiscDto();
		entry.m_name = "name";
		entry.m_timeOn = 10000;
		entry.m_timeRunning = 100000;
		entry.m_total = 2000000;
		entry.m_uid = 12;

		misc.add(entry);

		dto.m_refOther = misc;

		byte[] dtoBytes = dto.marshall();
		assertTrue(dtoBytes.length > 0);

		ReferenceDto dto2 = ReferenceDto.unmarshall(dtoBytes);
		assertTrue(dto2.m_fileName.equals(dto.m_fileName));
		assertTrue(dto2.m_refOther.size() > 0);

		ArrayList<MiscDto> misc2 = dto2.m_refOther;
		MiscDto entry2 = misc2.get(misc2.size() - 1);
		assertTrue(entry2.m_name.equals(entry.m_name));
		assertTrue(entry2.m_timeOn == entry.m_timeOn);
		assertTrue(entry2.m_timeRunning == entry.m_timeRunning);
		assertTrue(entry2.m_total == entry.m_total);
		assertTrue(entry2.m_uid == entry.m_uid);


	}

}