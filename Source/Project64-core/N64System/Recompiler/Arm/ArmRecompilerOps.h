/****************************************************************************
*                                                                           *
* Project64 - A Nintendo 64 emulator.                                      *
* http://www.pj64-emu.com/                                                  *
* Copyright (C) 2012 Project64. All rights reserved.                        *
*                                                                           *
* License:                                                                  *
* GNU/GPLv2 http://www.gnu.org/licenses/gpl-2.0.html                        *
*                                                                           *
****************************************************************************/
#pragma once
#if defined(__arm__) || defined(_M_ARM)
#include <Project64-core/N64System/Mips/RegisterClass.h>
#include <Project64-core/N64System/Recompiler/RecompilerOps.h>
#include <Project64-core/N64System/Recompiler/Arm/ArmOps.h>
#include <Project64-core/Settings/DebugSettings.h>

class CArmRecompilerOps :
    public CRecompilerOps,
    protected CDebugSettings,
    private CArmOps,
    protected CSystemRegisters
{
public:
    CArmRecompilerOps();

    /************************** Branch functions  ************************/
    void Compile_BranchCompare(BRANCH_COMPARE CompareType);
    void Compile_Branch(BRANCH_COMPARE CompareType, BRANCH_TYPE BranchType, bool Link);
    void Compile_BranchLikely(BRANCH_COMPARE CompareType, bool Link);
    void BNE_Compare();
    void BEQ_Compare();
    void BGTZ_Compare();
    void BLEZ_Compare();
    void BLTZ_Compare();
    void BGEZ_Compare();
    void COP1_BCF_Compare();
    void COP1_BCT_Compare();

    /*************************  OpCode functions *************************/
    void J              ();
    void JAL            ();
    void ADDI           ();
    void ADDIU          ();
    void SLTI           ();
    void SLTIU          ();
    void ANDI           ();
    void ORI            ();
    void XORI           ();
    void LUI            ();
    void DADDIU         ();
    void LDL            ();
    void LDR            ();
    void LB             ();
    void LH             ();
    void LWL            ();
    void LW             ();
    void LBU            ();
    void LHU            ();
    void LWR            ();
    void LWU            ();
    void SB             ();
    void SH             ();
    void SWL            ();
    void SW             ();
    void SWR            ();
    void SDL            ();
    void SDR            ();
    void CACHE          ();
    void LL             ();
    void LWC1           ();
    void LDC1           ();
    void LD             ();
    void SC             ();
    void SWC1           ();
    void SDC1           ();
    void SD             ();

    /********************** R4300i OpCodes: Special **********************/
    void SPECIAL_SLL    ();
    void SPECIAL_SRL    ();
    void SPECIAL_SRA    ();
    void SPECIAL_SLLV   ();
    void SPECIAL_SRLV   ();
    void SPECIAL_SRAV   ();
    void SPECIAL_JR     ();
    void SPECIAL_JALR   ();
    void SPECIAL_SYSCALL();
    void SPECIAL_MFLO   ();
    void SPECIAL_MTLO   ();
    void SPECIAL_MFHI   ();
    void SPECIAL_MTHI   ();
    void SPECIAL_DSLLV  ();
    void SPECIAL_DSRLV  ();
    void SPECIAL_DSRAV  ();
    void SPECIAL_MULT   ();
    void SPECIAL_MULTU  ();
    void SPECIAL_DIV    ();
    void SPECIAL_DIVU   ();
    void SPECIAL_DMULT  ();
    void SPECIAL_DMULTU ();
    void SPECIAL_DDIV   ();
    void SPECIAL_DDIVU  ();
    void SPECIAL_ADD    ();
    void SPECIAL_ADDU   ();
    void SPECIAL_SUB    ();
    void SPECIAL_SUBU   ();
    void SPECIAL_AND    ();
    void SPECIAL_OR     ();
    void SPECIAL_XOR    ();
    void SPECIAL_NOR    ();
    void SPECIAL_SLT    ();
    void SPECIAL_SLTU   ();
    void SPECIAL_DADD   ();
    void SPECIAL_DADDU  ();
    void SPECIAL_DSUB   ();
    void SPECIAL_DSUBU  ();
    void SPECIAL_DSLL   ();
    void SPECIAL_DSRL   ();
    void SPECIAL_DSRA   ();
    void SPECIAL_DSLL32 ();
    void SPECIAL_DSRL32 ();
    void SPECIAL_DSRA32 ();

    /************************** COP0 functions **************************/
    void COP0_MF        ();
    void COP0_MT        ();

    /************************** COP0 CO functions ***********************/
    void COP0_CO_TLBR   ();
    void COP0_CO_TLBWI  ();
    void COP0_CO_TLBWR  ();
    void COP0_CO_TLBP   ();
    void COP0_CO_ERET   ();

    /************************** COP1 functions **************************/
    void COP1_MF        ();
    void COP1_DMF       ();
    void COP1_CF        ();
    void COP1_MT        ();
    void COP1_DMT       ();
    void COP1_CT        ();

    /************************** COP1: S functions ************************/
    void COP1_S_ADD     ();
    void COP1_S_SUB     ();
    void COP1_S_MUL     ();
    void COP1_S_DIV     ();
    void COP1_S_ABS     ();
    void COP1_S_NEG     ();
    void COP1_S_SQRT    ();
    void COP1_S_MOV     ();
    void COP1_S_ROUND_L ();
    void COP1_S_TRUNC_L ();
    void COP1_S_CEIL_L  ();
    void COP1_S_FLOOR_L ();
    void COP1_S_ROUND_W ();
    void COP1_S_TRUNC_W ();
    void COP1_S_CEIL_W  ();
    void COP1_S_FLOOR_W ();
    void COP1_S_CVT_D   ();
    void COP1_S_CVT_W   ();
    void COP1_S_CVT_L   ();
    void COP1_S_CMP     ();

    /************************** COP1: D functions ************************/
    void COP1_D_ADD     ();
    void COP1_D_SUB     ();
    void COP1_D_MUL     ();
    void COP1_D_DIV     ();
    void COP1_D_ABS     ();
    void COP1_D_NEG     ();
    void COP1_D_SQRT    ();
    void COP1_D_MOV     ();
    void COP1_D_ROUND_L ();
    void COP1_D_TRUNC_L ();
    void COP1_D_CEIL_L  ();
    void COP1_D_FLOOR_L ();
    void COP1_D_ROUND_W ();
    void COP1_D_TRUNC_W ();
    void COP1_D_CEIL_W  ();
    void COP1_D_FLOOR_W ();
    void COP1_D_CVT_S   ();
    void COP1_D_CVT_W   ();
    void COP1_D_CVT_L   ();
    void COP1_D_CMP     ();

    /************************** COP1: W functions ************************/
    void COP1_W_CVT_S   ();
    void COP1_W_CVT_D   ();

    /************************** COP1: L functions ************************/
    void COP1_L_CVT_S   ();
    void COP1_L_CVT_D   ();

    /************************** Other functions **************************/
    void UnknownOpcode  ();

    void EnterCodeBlock();
    void ExitCodeBlock();
    void CompileExitCode();
    void CompileCop1Test();
    void CompileInPermLoop(CRegInfo & RegSet, uint32_t ProgramCounter);
    void SyncRegState(const CRegInfo & SyncTo);
    CRegInfo & GetRegWorkingSet(void);
    void SetRegWorkingSet(const CRegInfo & RegInfo);
    bool InheritParentInfo();
    void LinkJump(CJumpInfo & JumpInfo, uint32_t SectionID = -1, uint32_t FromSectionID = -1);
    void JumpToSection( CCodeSection * Section);
    void JumpToUnknown(CJumpInfo * JumpInfo);
    void SetCurrentPC(uint32_t ProgramCounter);
    uint32_t GetCurrentPC(void);
    void SetCurrentSection(CCodeSection * section);
    void SetNextStepType(STEP_TYPE StepType);
    STEP_TYPE GetNextStepType( void );
    const OPCODE & GetOpcode ( void ) const;
    void PreCompileOpcode(void);
    void PostCompileOpcode ( void );
    void CompileExit(uint32_t JumpPC, uint32_t TargetPC, CRegInfo &ExitRegSet, CExitInfo::EXIT_REASON reason);
    void CompileExit(uint32_t JumpPC, uint32_t TargetPC, CRegInfo &ExitRegSet, CExitInfo::EXIT_REASON reason, CArmOps::ArmBranchCompare CompareType);
    static void UpdateSyncCPU(CRegInfo & RegSet, uint32_t Cycles);
    void UpdateCounters(CRegInfo & RegSet, bool CheckTimer, bool ClearValues = false);
    void CompileSystemCheck(uint32_t TargetPC, const CRegInfo & RegSet);
    void CompileReadTLBMiss(ArmReg AddressReg, ArmReg LookUpReg);

    static inline uint32_t GetMipsRegLo(int32_t Reg) { return m_RegWorkingSet.GetMipsRegLo(Reg); }
    static inline int32_t GetMipsRegLo_S(int32_t Reg) { return m_RegWorkingSet.GetMipsRegLo_S(Reg); }
    static inline uint32_t GetMipsRegHi(int32_t Reg) { return m_RegWorkingSet.GetMipsRegHi(Reg); }
    static inline ArmReg GetMipsRegMapLo(int32_t Reg) { return m_RegWorkingSet.GetMipsRegMapLo(Reg); }
    static inline ArmReg GetMipsRegMapHi(int32_t Reg) { return m_RegWorkingSet.GetMipsRegMapHi(Reg); }

    static inline bool IsKnown(int32_t Reg) { return m_RegWorkingSet.IsKnown(Reg); }
    static inline bool IsUnknown(int32_t Reg) { return m_RegWorkingSet.IsUnknown(Reg); }
    static inline bool IsMapped(int32_t Reg) { return m_RegWorkingSet.IsMapped(Reg); }
    static inline bool IsConst(int32_t Reg) { return m_RegWorkingSet.IsConst(Reg); }
    static inline bool IsSigned(int32_t Reg) { return m_RegWorkingSet.IsSigned(Reg); }
    static inline bool Is32Bit(int32_t Reg) { return m_RegWorkingSet.Is32Bit(Reg); }
    static inline bool Is64Bit(int32_t Reg) { return m_RegWorkingSet.Is64Bit(Reg); }
    static inline void Map_GPR_32bit(int32_t Reg, bool SignValue, int32_t MipsRegToLoad) { m_RegWorkingSet.Map_GPR_32bit(Reg, SignValue, MipsRegToLoad); }
    static inline void Map_GPR_64bit(int32_t Reg, int32_t MipsRegToLoad) { m_RegWorkingSet.Map_GPR_64bit(Reg, MipsRegToLoad); }
    static inline void UnMap_GPR(uint32_t Reg, bool WriteBackValue){ m_RegWorkingSet.UnMap_GPR(Reg, WriteBackValue); }
    static inline ArmReg Map_TempReg(ArmReg Reg, int32_t MipsReg, bool LoadHiWord) { return m_RegWorkingSet.Map_TempReg(Reg, MipsReg, LoadHiWord); }
    static inline ArmReg Map_Variable(CArmRegInfo::VARIABLE_MAPPED variable) { return m_RegWorkingSet.Map_Variable(variable); }

    static inline void ResetRegProtection() { m_RegWorkingSet.ResetRegProtection(); }
    static inline void FixRoundModel(CRegInfo::FPU_ROUND RoundMethod) { m_RegWorkingSet.FixRoundModel(RoundMethod); }

    static inline void ProtectGPR(uint32_t Reg) { m_RegWorkingSet.ProtectGPR(Reg); }

private:
    void CompileInterpterCall (void * Function, const char * FunctionName);
    void OverflowDelaySlot(bool TestTimer);

    EXIT_LIST m_ExitInfo;
    STEP_TYPE m_NextInstruction;
    uint32_t m_CompilePC;
    OPCODE m_Opcode;
    CCodeSection * m_Section;
};

#endif