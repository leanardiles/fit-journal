package com.example.fitjournal_capstone_leandro.data.repository

import com.example.fitjournal_capstone_leandro.data.model.RoutineResponse

interface IUserRoutineRepository {
    suspend fun getRoutine(): Result<RoutineResponse>
    suspend fun saveRoutine(daysPerWeek: Int, routineDays: Map<Int, List<String>>): Result<Unit>
    suspend fun deleteRoutine(): Result<Unit>
}